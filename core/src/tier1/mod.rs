pub mod entropy;
pub mod homograph;
pub mod regex_rules;
pub mod static_rules;

use crate::qr::parse_qr_payload;
use crate::types::{ContentType, HeuristicResult, ThreatLevel};
use std::time::Instant;

pub type Tier1Result = HeuristicResult;

pub struct HeuristicsEngine;

impl HeuristicsEngine {
    pub fn new() -> Self {
        Self
    }

    /// Evaluates payload using Tier 1 local heuristics in < 5ms (typically < 1.5ms).
    pub fn evaluate(&self, content_type: ContentType, payload: &str) -> Tier1Result {
        let start = Instant::now();

        match content_type {
            ContentType::Url => {
                self.evaluate_url(payload, start)
            }
            ContentType::QrPayload => {
                self.evaluate_qr(payload, start)
            }
            ContentType::SmsText => {
                self.evaluate_sms(payload, start)
            }
            ContentType::ApkPermissions => {
                let perms: Vec<&str> = payload.lines().map(|s| s.trim()).filter(|s| !s.is_empty()).collect();
                self.evaluate_apk_permissions(&perms, start)
            }
        }
    }

    pub fn evaluate_qr(&self, payload: &str, start: Instant) -> Tier1Result {
        let parsed = parse_qr_payload(payload);
        
        if parsed.is_executable_risk {
            return Tier1Result {
                is_definitive: true,
                threat_level: ThreatLevel::Malicious,
                confidence: 0.99,
                category: "MALICIOUS_QR_SCHEME".to_string(),
                reason: parsed.warning.unwrap_or_else(|| "Dangerous executable scheme inside QR code".to_string()),
                latency_us: start.elapsed().as_micros() as u32,
                needs_tier2: false,
            };
        }

        if let Some(extracted_url) = parsed.extracted_url {
            let mut res = self.evaluate_url(&extracted_url, start);
            if res.threat_level == ThreatLevel::Malicious {
                res.category = format!("QUISHING_{}", res.category);
                res.reason = format!("Malicious QR code link: {}", res.reason);
            }
            return res;
        }

        // Plain text QR code
        self.evaluate_sms(&parsed.clean_text, start)
    }

    pub fn evaluate_apk_permissions(&self, permissions: &[&str], start: Instant) -> Tier1Result {
        let audit = static_rules::audit_permission_matrix(permissions);
        let latency_us = start.elapsed().as_micros() as u32;

        Tier1Result {
            is_definitive: true,
            threat_level: audit.threat_level,
            confidence: (audit.risk_score as f32) / 100.0,
            category: audit.category,
            reason: audit.explanation,
            latency_us,
            needs_tier2: false,
        }
    }

    fn evaluate_url(&self, payload: &str, start: Instant) -> Tier1Result {
        // 1. Dangerous scheme check (javascript:, data:)
        if let Some(finding) = static_rules::check_dangerous_schemes(payload) {
            return Tier1Result {
                is_definitive: true,
                threat_level: ThreatLevel::Malicious,
                confidence: finding.confidence,
                category: finding.category.to_string(),
                reason: finding.reason,
                latency_us: start.elapsed().as_micros() as u32,
                needs_tier2: false,
            };
        }

        // 2. Homoglyph / Punycode check
        let homo = homograph::detect_homograph(payload);
        if homo.is_threat {
            return Tier1Result {
                is_definitive: true,
                threat_level: ThreatLevel::Malicious,
                confidence: 0.98,
                category: "HOMOGRAPH".to_string(),
                reason: homo.reason,
                latency_us: start.elapsed().as_micros() as u32,
                needs_tier2: false,
            };
        }

        // 3. Regex structural check (Subdomain spoofing, IP host, Typosquats)
        if let Some(finding) = regex_rules::check_url_patterns(payload) {
            return Tier1Result {
                is_definitive: true,
                threat_level: ThreatLevel::Malicious,
                confidence: finding.confidence,
                category: finding.category.to_string(),
                reason: finding.reason,
                latency_us: start.elapsed().as_micros() as u32,
                needs_tier2: false,
            };
        }

        // 4. High-risk TLD check
        let tld_res = static_rules::check_high_risk_tld(payload);

        // 5. Shannon entropy analysis
        let (entropy_spike, _entropy_val, entropy_msg) = entropy::evaluate_url_entropy(payload);

        if let Some(tld) = tld_res {
            if entropy_spike {
                // High risk TLD + High entropy = definite malicious
                return Tier1Result {
                    is_definitive: true,
                    threat_level: ThreatLevel::Malicious,
                    confidence: 0.95,
                    category: "OBFUSCATED_PHISHING".to_string(),
                    reason: format!("{} Combined with: {}", tld.reason, entropy_msg),
                    latency_us: start.elapsed().as_micros() as u32,
                    needs_tier2: false,
                };
            } else {
                // High risk TLD alone is suspicious, triggers Tier 2 deep check
                return Tier1Result {
                    is_definitive: false,
                    threat_level: ThreatLevel::Suspicious,
                    confidence: tld.confidence,
                    category: tld.category.to_string(),
                    reason: tld.reason,
                    latency_us: start.elapsed().as_micros() as u32,
                    needs_tier2: true,
                };
            }
        }

        if entropy_spike {
            // High entropy without high-risk TLD triggers Tier 2
            return Tier1Result {
                is_definitive: false,
                threat_level: ThreatLevel::Suspicious,
                confidence: 0.75,
                category: "HIGH_ENTROPY".to_string(),
                reason: entropy_msg,
                latency_us: start.elapsed().as_micros() as u32,
                needs_tier2: true,
            };
        }

        // Clean URL
        Tier1Result {
            is_definitive: true,
            threat_level: ThreatLevel::Safe,
            confidence: 0.99,
            category: "SAFE".to_string(),
            reason: "Standard domain structure with authentic DNS characteristics and normal entropy.".to_string(),
            latency_us: start.elapsed().as_micros() as u32,
            needs_tier2: false,
        }
    }

    fn evaluate_sms(&self, text: &str, start: Instant) -> Tier1Result {
        // 1. If message contains a URL, scan the embedded URL first
        if let Some(idx) = text.find("http://").or_else(|| text.find("https://")) {
            let url_part = &text[idx..].split_whitespace().next().unwrap_or("");
            let url_res = self.evaluate_url(url_part, start);
            if url_res.threat_level == ThreatLevel::Malicious {
                return url_res;
            }
        }

        // 2. High-urgency regex check (Wire transfers, account suspension)
        if let Some(finding) = regex_rules::check_sms_patterns(text) {
            return Tier1Result {
                is_definitive: true,
                threat_level: ThreatLevel::Malicious,
                confidence: finding.confidence,
                category: finding.category.to_string(),
                reason: finding.reason,
                latency_us: start.elapsed().as_micros() as u32,
                needs_tier2: false,
            };
        }

        // 3. Ambiguity check: check if conversational or needs deep transformer NLP
        let lower = text.to_lowercase();
        let ambiguous_keywords = [
            "passcode", "code", "pin", "otp", "billing", "rewards", "claim",
            "points", "dropbox", "share", "survey", "prize", "winner", "order"
        ];

        let has_ambiguity = ambiguous_keywords.iter().any(|&k| lower.contains(k));

        if has_ambiguity {
            return Tier1Result {
                is_definitive: false,
                threat_level: ThreatLevel::Suspicious,
                confidence: 0.65,
                category: "SUSPICIOUS_SMS".to_string(),
                reason: "Message contains ambiguous security or transactional keywords; escalating to Tier 2 Transformer.".to_string(),
                latency_us: start.elapsed().as_micros() as u32,
                needs_tier2: true,
            };
        }

        // Clean conversational text
        Tier1Result {
            is_definitive: true,
            threat_level: ThreatLevel::Safe,
            confidence: 0.99,
            category: "SAFE".to_string(),
            reason: "Standard conversational message with zero coercive urgency or credential requests.".to_string(),
            latency_us: start.elapsed().as_micros() as u32,
            needs_tier2: false,
        }
    }
}
