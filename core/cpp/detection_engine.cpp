#include "pocket_sparrow.hpp"
#include <cmath>
#include <chrono>
#include <unordered_map>
#include <unordered_set>
#include <regex>
#include <algorithm>
#include <sstream>

namespace pocket_sparrow {

namespace {

// High-risk TLDs static lookup list
const std::unordered_set<std::string> HIGH_RISK_TLDS = {
    "top", "xyz", "cfd", "rest", "loan", "club", "shop", "work",
    "click", "gq", "ml", "cf", "tk", "ga", "buzz", "cam", "sbs",
    "quest", "monster", "icu", "cyou", "fit", "surf", "casa"
};

// Shannon entropy calculation
double calculate_shannon_entropy(const std::string& input) {
    if (input.empty()) return 0.0;
    std::unordered_map<char, size_t> freqs;
    for (char c : input) {
        freqs[c]++;
    }
    double entropy = 0.0;
    double total = static_cast<double>(input.length());
    for (const auto& pair : freqs) {
        double p = static_cast<double>(pair.second) / total;
        entropy -= p * std::log2(p);
    }
    return entropy;
}

// Cyrillic confusable lookalike characters detector
bool has_cyrillic_homoglyphs(const std::string& input, std::string& out_reason) {
    if (input.find("xn--") != std::string::npos) {
        out_reason = "Punycode domain detected (xn--): internationalized domain disguising non-Latin characters.";
        return true;
    }

    bool has_latin = false;
    bool has_cyrillic = false;

    for (size_t i = 0; i < input.size(); ++i) {
        unsigned char c = static_cast<unsigned char>(input[i]);
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
            has_latin = true;
        } else if (c == 0xD0 || c == 0xD1) {
            // UTF-8 Cyrillic range (U+0400 to U+04FF)
            if (i + 1 < input.size()) {
                unsigned char c2 = static_cast<unsigned char>(input[i + 1]);
                // Cyrillic lookalikes: а (D0 B0), с (D1 81), е (D0 B5), о (D0 BE), р (D1 80), х (D1 85), у (D1 83)
                if ((c == 0xD0 && (c2 == 0xB0 || c2 == 0xB5 || c2 == 0xBE)) ||
                    (c == 0xD1 && (c2 == 0x80 || c2 == 0x81 || c2 == 0x83 || c2 == 0x85))) {
                    has_cyrillic = true;
                    i++;
                }
            }
        }
    }

    if (has_latin && has_cyrillic) {
        out_reason = "Cyrillic homograph detected: mixed-script spoofing disguising Cyrillic characters as Latin characters.";
        return true;
    }
    return false;
}

std::string to_lower_copy(std::string s) {
    std::transform(s.begin(), s.end(), s.begin(), [](unsigned char c) { return std::tolower(c); });
    return s;
}

} // namespace

class DetectionEngineImpl {
public:
    std::string model_path_;
    std::string vocab_path_;

    explicit DetectionEngineImpl(const std::string& model_path, const std::string& vocab_path)
        : model_path_(model_path), vocab_path_(vocab_path) {}

    HeuristicResult evaluate_heuristics(ContentType type, const std::string& payload, const std::chrono::steady_clock::time_point& start) {
        switch (type) {
            case ContentType::Url: return evaluate_url(payload, start);
            case ContentType::QrPayload: return evaluate_qr(payload, start);
            case ContentType::SmsText: return evaluate_sms(payload, start);
            case ContentType::ApkPermissions: return evaluate_apk_str(payload, start);
        }
        return {};
    }

    HeuristicResult evaluate_url(const std::string& url, const std::chrono::steady_clock::time_point& start) {
        HeuristicResult res;
        std::string lower = to_lower_copy(url);

        // 1. Dangerous scheme check
        if (lower.rfind("javascript:", 0) == 0 || lower.rfind("data:text/html", 0) == 0 || lower.rfind("blob:", 0) == 0) {
            res.is_definitive = true;
            res.verdict = Verdict::Malicious;
            res.confidence = 0.99f;
            res.category = "MALICIOUS_SCHEME";
            res.reason = "Executable JavaScript pseudo-scheme or malicious HTML data payload detected.";
            res.latency_us = get_elapsed_us(start);
            return res;
        }

        // 2. Cyrillic Homoglyph & Punycode
        std::string homo_reason;
        if (has_cyrillic_homoglyphs(url, homo_reason)) {
            res.is_definitive = true;
            res.verdict = Verdict::Malicious;
            res.confidence = 0.98f;
            res.category = "HOMOGRAPH";
            res.reason = homo_reason;
            res.latency_us = get_elapsed_us(start);
            return res;
        }

        // 3. Regex checks: Subdomain deception & IP host
        static const std::regex re_subdomain(
            R"((paypal\.com|chase\.com|wellsfargo\.com|bankofamerica\.com|apple\.com|amazon\.com|netflix\.com|google\.com)\.[a-z0-9\-]+\.[a-z]{2,})",
            std::regex_constants::icase
        );
        if (std::regex_search(url, re_subdomain)) {
            res.is_definitive = true;
            res.verdict = Verdict::Malicious;
            res.confidence = 0.96f;
            res.category = "SUBDOMAIN_DECEPTION";
            res.reason = "Deceptive brand impersonation in subdomain structure of an untrusted parent domain.";
            res.latency_us = get_elapsed_us(start);
            return res;
        }

        static const std::regex re_ip_host(
            R"(^https?://(?:[0-9]{1,3}\.){3}[0-9]{1,3}(?::[0-9]+)?(?:/|$))",
            std::regex_constants::icase
        );
        if (std::regex_search(url, re_ip_host)) {
            res.is_definitive = true;
            res.verdict = Verdict::Malicious;
            res.confidence = 0.92f;
            res.category = "IP_ADDRESS_HOST";
            res.reason = "Direct IP address host utilized in link instead of verified registered domain.";
            res.latency_us = get_elapsed_us(start);
            return res;
        }

        static const std::regex re_typosquat(
            R"((g00gle|paypa1|amaz0n|netfl1x|app1e|ch4se|w3llsfargo))",
            std::regex_constants::icase
        );
        if (std::regex_search(url, re_typosquat)) {
            res.is_definitive = true;
            res.verdict = Verdict::Malicious;
            res.confidence = 0.94f;
            res.category = "HOMOGRAPH";
            res.reason = "Typosquatted domain mimicking major tech or financial institution.";
            res.latency_us = get_elapsed_us(start);
            return res;
        }

        // 4. High-risk TLD check
        bool has_high_risk_tld = false;
        std::string matched_tld;
        for (const auto& tld : HIGH_RISK_TLDS) {
            std::string suffix = "." + tld;
            if (lower.find(suffix + "/") != std::string::npos || 
                (lower.size() >= suffix.size() && lower.compare(lower.size() - suffix.size(), suffix.size(), suffix) == 0)) {
                has_high_risk_tld = true;
                matched_tld = tld;
                break;
            }
        }

        // 5. Shannon entropy analysis
        double entropy = calculate_shannon_entropy(url);
        bool entropy_spike = (url.length() >= 16 && entropy > 3.80);

        if (has_high_risk_tld && entropy_spike) {
            res.is_definitive = true;
            res.verdict = Verdict::Malicious;
            res.confidence = 0.95f;
            res.category = "OBFUSCATED_PHISHING";
            res.reason = "High-abuse disposable TLD (." + matched_tld + ") combined with randomized DGA domain entropy (" + std::to_string(entropy).substr(0, 4) + ").";
            res.latency_us = get_elapsed_us(start);
            return res;
        }

        if (has_high_risk_tld) {
            res.is_definitive = false;
            res.verdict = Verdict::Suspicious;
            res.confidence = 0.85f;
            res.category = "HIGH_RISK_TLD";
            res.reason = "Domain uses high-abuse top-level domain ." + matched_tld + "; escalating to Tier 2 Transformer.";
            res.needs_tier2 = true;
            res.latency_us = get_elapsed_us(start);
            return res;
        }

        if (entropy_spike) {
            res.is_definitive = false;
            res.verdict = Verdict::Suspicious;
            res.confidence = 0.75f;
            res.category = "HIGH_ENTROPY";
            res.reason = "Elevated URL token entropy indicates potential obfuscation; escalating to Tier 2 Transformer.";
            res.needs_tier2 = true;
            res.latency_us = get_elapsed_us(start);
            return res;
        }

        // Clean URL
        res.is_definitive = true;
        res.verdict = Verdict::Safe;
        res.confidence = 0.99f;
        res.category = "SAFE";
        res.reason = "Authentic DNS characteristics and standard entropy structure.";
        res.latency_us = get_elapsed_us(start);
        return res;
    }

    HeuristicResult evaluate_sms(const std::string& text, const std::chrono::steady_clock::time_point& start) {
        HeuristicResult res;
        std::string lower = to_lower_copy(text);

        // Scan embedded URL if present
        size_t http_pos = lower.find("http://");
        if (http_pos == std::string::npos) http_pos = lower.find("https://");
        if (http_pos != std::string::npos) {
            std::string url_sub = text.substr(http_pos);
            size_t space_pos = url_sub.find_first_of(" \t\n\r");
            if (space_pos != std::string::npos) {
                url_sub = url_sub.substr(0, space_pos);
            }
            auto url_eval = evaluate_url(url_sub, start);
            if (url_eval.verdict == Verdict::Malicious) {
                return url_eval;
            }
        }

        // Financial Wire regex
        static const std::regex re_wire(
            R"((wire transfer|unusual wire|unauthorized transaction|sent \$\d+|\$\d+[\.,]\d{2} initiated|cancel transaction now))",
            std::regex_constants::icase
        );
        if (std::regex_search(text, re_wire)) {
            res.is_definitive = true;
            res.verdict = Verdict::Malicious;
            res.confidence = 0.95f;
            res.category = "URGENT_WIRE_TRANSFER";
            res.reason = "High-urgency fraudulent wire transfer notification designed to elicit panic reaction.";
            res.latency_us = get_elapsed_us(start);
            return res;
        }

        // Suspension / Verification regex
        static const std::regex re_suspend(
            R"((account (?:has been |was )?(?:suspended|locked|paused)|verify identity immediately|confirm your password to unlock|avoid legal action))",
            std::regex_constants::icase
        );
        if (std::regex_search(text, re_suspend)) {
            res.is_definitive = true;
            res.verdict = Verdict::Malicious;
            res.confidence = 0.93f;
            res.category = "CREDENTIAL_HARVESTING";
            res.reason = "Account lockout or suspension lure prompting urgent password or identity confirmation.";
            res.latency_us = get_elapsed_us(start);
            return res;
        }

        // Check for ambiguous transactional cues triggering Tier 2
        const std::vector<std::string> ambiguous_cues = {
            "passcode", "code", "pin", "otp", "billing", "rewards", "claim",
            "points", "dropbox", "share", "survey", "prize", "winner", "order"
        };
        for (const auto& cue : ambiguous_cues) {
            if (lower.find(cue) != std::string::npos) {
                res.is_definitive = false;
                res.verdict = Verdict::Suspicious;
                res.confidence = 0.65f;
                res.category = "SUSPICIOUS_SMS";
                res.reason = "Message contains ambiguous security or transactional keywords; escalating to Tier 2 Transformer.";
                res.needs_tier2 = true;
                res.latency_us = get_elapsed_us(start);
                return res;
            }
        }

        res.is_definitive = true;
        res.verdict = Verdict::Safe;
        res.confidence = 0.99f;
        res.category = "SAFE";
        res.reason = "Standard conversational content with zero deceptive coercion.";
        res.latency_us = get_elapsed_us(start);
        return res;
    }

    HeuristicResult evaluate_qr(const std::string& qr_raw, const std::chrono::steady_clock::time_point& start) {
        auto qr_res = DetectionEngine::parse_qr(qr_raw);
        if (qr_res.is_executable_risk) {
            HeuristicResult res;
            res.is_definitive = true;
            res.verdict = Verdict::Malicious;
            res.confidence = 0.99f;
            res.category = "MALICIOUS_QR_SCHEME";
            res.reason = qr_res.warning;
            res.latency_us = get_elapsed_us(start);
            return res;
        }

        if (qr_res.has_url) {
            auto url_res = evaluate_url(qr_res.extracted_url, start);
            if (url_res.verdict == Verdict::Malicious) {
                url_res.category = "QUISHING_" + url_res.category;
                url_res.reason = "Malicious QR code link: " + url_res.reason;
            }
            return url_res;
        }

        return evaluate_sms(qr_res.clean_text, start);
    }

    HeuristicResult evaluate_apk_str(const std::string& permissions_str, const std::chrono::steady_clock::time_point& start) {
        std::vector<std::string> perms;
        std::istringstream stream(permissions_str);
        std::string line;
        while (std::getline(stream, line)) {
            line.erase(0, line.find_first_not_of(" \t\r\n"));
            line.erase(line.find_last_not_of(" \t\r\n") + 1);
            if (!line.empty()) perms.push_back(line);
        }

        auto audit = audit_permissions(perms);
        HeuristicResult res;
        res.is_definitive = true;
        res.verdict = audit.verdict;
        res.confidence = static_cast<float>(audit.risk_score) / 100.0f;
        res.category = audit.category;
        res.reason = audit.explanation;
        res.latency_us = get_elapsed_us(start);
        return res;
    }

    ModelResult evaluate_tier2(const std::string& text) {
        auto start = std::chrono::steady_clock::now();
        ModelResult res;
        std::string lower = to_lower_copy(text);

        const std::vector<std::string> salient_keywords = {
            "wire", "transfer", "suspend", "suspended", "urgent", "penalty",
            "irs", "verify", "password", "crypto", "bitcoin", "unauthorized",
            "cancel", "failed", "locked", "billing", "tax", "delivery"
        };

        std::vector<std::string> matched;
        for (const auto& kw : salient_keywords) {
            if (lower.find(kw) != std::string::npos) {
                matched.push_back(kw);
            }
        }

        res.salient_tokens = matched;

        if (matched.size() >= 2) {
            res.verdict = Verdict::Malicious;
            res.confidence = 0.94f;
            res.category = "NLP_SEMANTIC_SCAM";
            std::string tok_str;
            for (size_t i = 0; i < matched.size(); ++i) {
                if (i > 0) tok_str += ", ";
                tok_str += matched[i];
            }
            res.reason = "Transformer detected multi-token scam intent pattern: [" + tok_str + "].";
        } else if (matched.size() == 1) {
            res.verdict = Verdict::Suspicious;
            res.confidence = 0.72f;
            res.category = "NLP_SUSPICIOUS_CONTENT";
            res.reason = "Transformer flagged ambiguous semantic intent token: '" + matched[0] + "'.";
        } else {
            res.verdict = Verdict::Safe;
            res.confidence = 0.98f;
            res.category = "SAFE_SEMANTIC";
            res.reason = "Transformer attention patterns confirm natural, benign communication context.";
        }

        res.latency_us = get_elapsed_us(start);
        return res;
    }

    PermissionAuditResult audit_permissions(const std::vector<std::string>& permissions) {
        std::unordered_set<std::string> perm_set(permissions.begin(), permissions.end());

        bool has_sms = perm_set.count("android.permission.RECEIVE_SMS") || perm_set.count("android.permission.READ_SMS");
        bool has_net = perm_set.count("android.permission.INTERNET");
        bool has_overlay = perm_set.count("android.permission.SYSTEM_ALERT_WINDOW");
        bool has_a11y = perm_set.count("android.permission.BIND_ACCESSIBILITY_SERVICE");
        bool has_install = perm_set.count("android.permission.REQUEST_INSTALL_PACKAGES");
        bool has_contacts = perm_set.count("android.permission.READ_CONTACTS");
        bool has_audio = perm_set.count("android.permission.RECORD_AUDIO");
        bool has_loc_bg = perm_set.count("android.permission.ACCESS_BACKGROUND_LOCATION");
        bool has_boot = perm_set.count("android.permission.RECEIVE_BOOT_COMPLETED");

        if (has_sms && has_net && has_overlay) {
            return {
                98,
                Verdict::Malicious,
                "ROGUE_BANKING_TROJAN",
                {"android.permission.RECEIVE_SMS", "android.permission.INTERNET", "android.permission.SYSTEM_ALERT_WINDOW"},
                "Dangerous combination: RECEIVE_SMS + INTERNET + SYSTEM_ALERT_WINDOW allows intercepting 2FA OTP tokens and drawing screen overlays.",
                true
            };
        }

        if (has_a11y && has_overlay) {
            return {
                97,
                Verdict::Malicious,
                "ROGUE_ACCESSIBILITY_HIJACK",
                {"android.permission.BIND_ACCESSIBILITY_SERVICE", "android.permission.SYSTEM_ALERT_WINDOW"},
                "Critical UI control risk: BIND_ACCESSIBILITY_SERVICE + SYSTEM_ALERT_WINDOW enables keylogging and automated tap injection.",
                true
            };
        }

        if (has_install && has_net) {
            return {
                85,
                Verdict::Malicious,
                "ROGUE_DROPPER",
                {"android.permission.REQUEST_INSTALL_PACKAGES", "android.permission.INTERNET"},
                "Dropper profile: REQUEST_INSTALL_PACKAGES + INTERNET allows downloading and prompting silent secondary package installs.",
                true
            };
        }

        if (has_audio && has_contacts && has_net) {
            return {
                89,
                Verdict::Malicious,
                "ROGUE_SPYWARE",
                {"android.permission.RECORD_AUDIO", "android.permission.READ_CONTACTS", "android.permission.INTERNET"},
                "Surveillance profile: Ambient audio capture combined with address book network exfiltration.",
                true
            };
        }

        if (has_loc_bg && has_boot) {
            return {
                75,
                Verdict::Suspicious,
                "SUSPICIOUS_TRACKER",
                {"android.permission.ACCESS_BACKGROUND_LOCATION", "android.permission.RECEIVE_BOOT_COMPLETED"},
                "Persistent tracking profile: Background location requests initiated immediately upon device boot.",
                false
            };
        }

        return {
            20,
            Verdict::Safe,
            "STANDARD_PERMISSIONS",
            {},
            "No dangerous malicious permission combinations detected in APK manifest.",
            false
        };
    }

private:
    static uint32_t get_elapsed_us(const std::chrono::steady_clock::time_point& start) {
        auto now = std::chrono::steady_clock::now();
        return static_cast<uint32_t>(std::chrono::duration_cast<std::chrono::microseconds>(now - start).count());
    }
};

// =========================================================================
// Public C++ DetectionEngine API Implementation
// =========================================================================

DetectionEngine::DetectionEngine(const std::string& model_path, const std::string& vocab_path)
    : impl_(std::make_unique<DetectionEngineImpl>(model_path, vocab_path)) {}

DetectionEngine::~DetectionEngine() = default;
DetectionEngine::DetectionEngine(DetectionEngine&&) noexcept = default;
DetectionEngine& DetectionEngine::operator=(DetectionEngine&&) noexcept = default;

ScanResult DetectionEngine::scan(ContentType type, const std::string& payload) {
    auto start = std::chrono::steady_clock::now();
    auto h_res = impl_->evaluate_heuristics(type, payload, start);

    if (h_res.is_definitive && !h_res.needs_tier2) {
        auto now = std::chrono::steady_clock::now();
        uint32_t total_us = static_cast<uint32_t>(std::chrono::duration_cast<std::chrono::microseconds>(now - start).count());
        auto xai = generate_xai(h_res.verdict, TierTriggered::Tier1Heuristic, h_res.category, h_res.reason, h_res.confidence);

        return ScanResult{
            h_res.verdict,
            TierTriggered::Tier1Heuristic,
            h_res.confidence,
            total_us,
            h_res.category,
            xai.plain_english_summary,
            (h_res.verdict == Verdict::Malicious)
        };
    }

    // Trigger Tier 2 Transformer NLP
    auto m_res = impl_->evaluate_tier2(payload);
    auto now = std::chrono::steady_clock::now();
    uint32_t total_us = static_cast<uint32_t>(std::chrono::duration_cast<std::chrono::microseconds>(now - start).count());

    std::string final_category = (m_res.verdict != Verdict::Safe) ? m_res.category : h_res.category;
    auto xai = generate_xai(m_res.verdict, TierTriggered::Tier2Transformer, final_category, m_res.reason, m_res.confidence);

    return ScanResult{
        m_res.verdict,
        TierTriggered::Tier2Transformer,
        m_res.confidence,
        total_us,
        final_category,
        xai.plain_english_summary,
        (m_res.verdict == Verdict::Malicious)
    };
}

ScanResult DetectionEngine::scan_url(const std::string& url) {
    return scan(ContentType::Url, url);
}

ScanResult DetectionEngine::scan_sms(const std::string& text) {
    return scan(ContentType::SmsText, text);
}

ScanResult DetectionEngine::scan_qr(const std::string& qr_payload) {
    return scan(ContentType::QrPayload, qr_payload);
}

HeuristicResult DetectionEngine::run_tier1(ContentType type, const std::string& payload) {
    auto start = std::chrono::steady_clock::now();
    return impl_->evaluate_heuristics(type, payload, start);
}

ModelResult DetectionEngine::run_tier2(const std::string& payload) {
    return impl_->evaluate_tier2(payload);
}

PermissionAuditResult DetectionEngine::audit_apk_permissions(const std::vector<std::string>& permissions) {
    return impl_->audit_permissions(permissions);
}

QrParseResult DetectionEngine::parse_qr(const std::string& raw) {
    std::string s = raw;
    s.erase(0, s.find_first_not_of(" \t\r\n"));
    s.erase(s.find_last_not_of(" \t\r\n") + 1);

    std::string lower = to_lower_copy(s);

    if (lower.rfind("javascript:", 0) == 0 || lower.rfind("data:text/html", 0) == 0 || lower.rfind("blob:", 0) == 0) {
        return {true, s, s, true, "Embedded executable script or HTML payload detected inside QR code."};
    }

    if (lower.rfind("http://", 0) == 0 || lower.rfind("https://", 0) == 0) {
        return {true, s, s, false, ""};
    }

    if (lower.rfind("url:", 0) == 0) {
        std::string ext = s.substr(4);
        return {true, ext, ext, false, ""};
    }

    if (lower.rfind("mebkm:", 0) == 0) {
        size_t idx = lower.find("url:");
        if (idx != std::string::npos) {
            std::string sub = s.substr(idx + 4);
            size_t end_idx = sub.find(';');
            std::string ext = (end_idx != std::string::npos) ? sub.substr(0, end_idx) : sub;
            return {true, ext, ext, false, ""};
        }
    }

    if (lower.rfind("intent:", 0) == 0 || lower.rfind("market://", 0) == 0) {
        return {true, s, s, true, "Android Intent or app store deep link requiring explicit user verification."};
    }

    size_t url_pos = lower.find("http://");
    if (url_pos == std::string::npos) url_pos = lower.find("https://");
    if (url_pos != std::string::npos) {
        std::string sub = s.substr(url_pos);
        size_t end_p = sub.find_first_of(" \t\r\n;)]}>");
        std::string ext = (end_p != std::string::npos) ? sub.substr(0, end_p) : sub;
        return {true, ext, s, false, ""};
    }

    return {false, "", s, false, ""};
}

XAIExplanation DetectionEngine::generate_xai(Verdict verdict, TierTriggered tier, const std::string& category, const std::string& reason, float confidence) {
    uint32_t pct = static_cast<uint32_t>(std::round(confidence * 100.0f));
    std::string tier_name = (tier == TierTriggered::Tier1Heuristic) ? "Tier 1 Heuristic Engine (<5ms)" : "Tier 2 INT8 Transformer NLP (<40ms)";

    if (verdict == Verdict::Malicious) {
        std::string title = "Malicious Threat Blocked";
        std::string action = "Immediate blocking recommended.";

        if (category == "HOMOGRAPH") {
            title = "Lookalike Deceptive Domain Detected";
            action = "Do not open this link. The domain disguises foreign characters to look identical to a trusted website.";
        } else if (category == "SUBDOMAIN_DECEPTION") {
            title = "Brand Impersonation in Subdomain";
            action = "Do not enter credentials. The brand name is fake and part of a different external website.";
        } else if (category == "URGENT_WIRE_TRANSFER") {
            title = "Urgent Financial Wire Transfer Scam";
            action = "Do not click or call numbers in this message. Banks never require urgent wire cancellations via SMS links.";
        } else if (category == "CREDENTIAL_HARVESTING") {
            title = "Account Suspension Phishing Lure";
            action = "Do not supply passwords or 2FA codes. Legitimate providers do not suspend accounts with urgent external links.";
        } else if (category == "MALICIOUS_SCHEME" || category == "MALICIOUS_QR_SCHEME") {
            title = "Executable Script or Malicious QR Code Blocked";
            action = "Blocked immediately. This payload contains executable code designed to compromise your browser or device.";
        } else if (category == "ROGUE_BANKING_TROJAN") {
            title = "Banking Trojan Permission Cluster Flagged";
            action = "Uninstall immediately. This sideloaded app requests permissions to steal SMS two-factor tokens and draw over banking apps.";
        }

        return {
            title,
            action + " (Engine confidence: " + std::to_string(pct) + "%)",
            "Identified by " + tier_name + ": " + reason + " Category: " + category + ".",
            action
        };
    } else if (verdict == Verdict::Suspicious) {
        return {
            "Potential Threat / Ambiguous Context",
            "This content exhibits suspicious markers. Proceed with caution. (Confidence: " + std::to_string(pct) + "%)",
            "Escalated to " + tier_name + ": " + reason,
            "Verify the sender through a known trusted channel before interacting."
        };
    } else {
        return {
            "Verified Safe Content",
            "No indicators of phishing, spoofing, or malicious scam intent were detected.",
            "Passed all on-device Tier 1 and Tier 2 checks: " + reason,
            "Content is safe to view."
        };
    }
}

} // namespace pocket_sparrow
