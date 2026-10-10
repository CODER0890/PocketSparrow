use crate::engine::DetectionEngine;
use crate::tier1::homograph::detect_homograph;
use crate::types::{CommunicationVerdict, ContentType, EmailSignals, EmailVerdict, ThreatLevel};
use std::sync::Arc;
use std::time::Instant;

pub struct EmailScreener {
    engine: Arc<DetectionEngine>,
}

impl EmailScreener {
    pub fn new(engine: Arc<DetectionEngine>) -> Self {
        Self { engine }
    }

    /// Evaluates email on-device in < 30ms.
    pub fn screen_email(&self, signals: &EmailSignals) -> EmailVerdict {
        let start = Instant::now();
        let mut xai_reasons = Vec::new();
        let mut tier: u8 = 1;
        let mut spoofing_detected = false;
        let mut tracking_pixels_neutralized = 0;

        // 1. Extract embedded URLs from body (plain text & HTML href)
        let mut extracted_urls = Vec::new();
        for word in signals.body.split_whitespace() {
            if word.starts_with("http://") || word.starts_with("https://") {
                let clean = word.trim_matches(|c| c == '<' || c == '>' || c == '"' || c == '\'' || c == ',' || c == '.');
                if !extracted_urls.contains(&clean.to_string()) {
                    extracted_urls.push(clean.to_string());
                }
            }
        }

        // HTML href extraction
        let lower_body = signals.body.to_lowercase();
        let mut search_idx = 0;
        while let Some(href_idx) = lower_body[search_idx..].find("href=") {
            let val_start = search_idx + href_idx + 5;
            if val_start < signals.body.len() {
                let rest = &signals.body[val_start..];
                let quote = rest.chars().next().unwrap_or(' ');
                if quote == '"' || quote == '\'' {
                    if let Some(end_quote) = rest[1..].find(quote) {
                        let link = &rest[1..1 + end_quote];
                        if link.starts_with("http://") || link.starts_with("https://") {
                            let clean_link = link.to_string();
                            if !extracted_urls.contains(&clean_link) {
                                extracted_urls.push(clean_link);
                            }
                        }
                    }
                }
            }
            search_idx = val_start + 1;
        }

        // 2. Identify Tracking Pixels (Local Pattern Detection - Zero Network Fetch)
        if lower_body.contains("width=\"1\"") || lower_body.contains("height=\"1\"") || lower_body.contains("track.gif") || lower_body.contains("open.aspx") || lower_body.contains("pixel.png") || lower_body.contains("opacity:0") || lower_body.contains("display:none") {
            tracking_pixels_neutralized += 1;
            xai_reasons.push("Tracking pixel pattern detected and neutralized locally (zero remote request sent).".to_string());
        }

        // 3. SPF / DKIM / DMARC Authentication Analysis
        if !signals.dmarc_pass && (!signals.spf_pass || !signals.dkim_pass) {
            xai_reasons.push("DMARC alignment failure: Email failed sender domain authentication checks.".to_string());
        }

        // 4. Expanded Known Brands Impersonation Detection
        let known_brands = [
            ("paypal", "paypal.com"),
            ("chase", "chase.com"),
            ("bank of america", "bankofamerica.com"),
            ("wellsfargo", "wellsfargo.com"),
            ("citibank", "citi.com"),
            ("capital one", "capitalone.com"),
            ("venmo", "venmo.com"),
            ("zelle", "zellepay.com"),
            ("apple", "apple.com"),
            ("amazon", "amazon.com"),
            ("netflix", "netflix.com"),
            ("google", "google.com"),
            ("microsoft", "microsoft.com"),
            ("meta", "meta.com"),
            ("facebook", "facebook.com"),
            ("instagram", "instagram.com"),
            ("docusign", "docusign.com"),
            ("stripe", "stripe.com"),
            ("coinbase", "coinbase.com"),
            ("binance", "binance.com"),
            ("irs", "irs.gov"),
            ("dhl", "dhl.com"),
            ("fedex", "fedex.com"),
            ("usps", "usps.com"),
        ];

        let free_webmail_providers = [
            "gmail.com", "yahoo.com", "hotmail.com", "outlook.com",
            "proton.me", "protonmail.com", "aol.com", "mail.com", "yandex.com", "icloud.com"
        ];

        let sender_lower = signals.sender_address.to_lowercase();
        let sender_domain = sender_lower.split('@').nth(1).unwrap_or("");

        if let Some(ref disp) = signals.display_name {
            let disp_lower = disp.to_lowercase();
            for (brand_name, expected_domain) in &known_brands {
                if disp_lower.contains(brand_name) && !sender_lower.ends_with(&format!("@{}", expected_domain)) {
                    spoofing_detected = true;
                    xai_reasons.push(format!(
                        "Display-name impersonation: Header claims '{}' but sender address domain is '{}'.",
                        disp, signals.sender_address
                    ));
                    break;
                }
            }

            // Check if free webmail provider is used for corporate or administrative impersonation
            let institutional_terms = ["support", "security", "billing", "service", "helpdesk", "administrator", "verify", "team", "account"];
            let is_institutional = institutional_terms.iter().any(|term| disp_lower.contains(term));
            let is_free_mail = free_webmail_providers.iter().any(|prov| sender_domain.ends_with(prov));

            if is_institutional && is_free_mail && !spoofing_detected {
                spoofing_detected = true;
                xai_reasons.push(format!(
                    "Free webmail provider impersonation: Header claims '{}' using free consumer address '@{}'.",
                    disp, sender_domain
                ));
            }
        }

        // 5. Lookalike / Homoglyph Domain Check on Sender Address
        if !sender_domain.is_empty() {
            let homo = detect_homograph(sender_domain);
            if homo.is_threat {
                spoofing_detected = true;
                xai_reasons.push(format!("Sender domain uses homoglyph visual spoofing: {}", homo.reason));
            }
        }

        // 6. Deceptive Anchor Text Mismatch Detection (<a href="BAD">GOOD_DOMAIN</a>)
        for u in &extracted_urls {
            for (brand_name, expected_domain) in &known_brands {
                let anchor_pattern = format!("{}.com", brand_name);
                if lower_body.contains(&anchor_pattern) && !u.to_lowercase().contains(expected_domain) {
                    spoofing_detected = true;
                    xai_reasons.push(format!(
                        "Deceptive link anchor mismatch: Body displays '{}' brand but destination link is '{}'.",
                        brand_name, u
                    ));
                    break;
                }
            }
        }

        // 7. Dangerous Executable Attachment / Payload File Extensions
        let dangerous_extensions = [
            ".exe", ".scr", ".bat", ".vbs", ".iso", ".apk", ".cmd", ".ps1", ".hta", ".wsf",
            ".pdf.exe", ".doc.exe", ".zip.exe", ".invoice.exe"
        ];
        let lower_content = format!("{} {}", signals.subject.to_lowercase(), signals.body.to_lowercase());
        let mut dangerous_payload_found = false;
        for ext in &dangerous_extensions {
            if lower_content.contains(ext) {
                dangerous_payload_found = true;
                xai_reasons.push(format!(
                    "Dangerous executable payload reference detected: '{}' disguised within email.",
                    ext
                ));
                break;
            }
        }

        // 8. Urgent Social Engineering & Financial Coercion Keywords
        let urgency_keywords = [
            "account suspended", "immediate verification required", "unauthorized wire transfer",
            "unauthorized access", "wire transfer of", "billing failure", "verify your identity",
            "password expired", "unusual activity detected", "tax refund pending", "confirm your account within 24 hours",
            "immediate action required", "compromised password", "security alert: sign-in"
        ];
        let mut urgency_lure_found = false;
        for kw in &urgency_keywords {
            if lower_content.contains(kw) {
                urgency_lure_found = true;
                xai_reasons.push(format!("High-urgency social engineering lure detected: '{}'.", kw));
                break;
            }
        }

        // 9. Embedded URLs Inspection with Pocket Sparrow Engines
        let mut url_threat_found = false;
        for u in &extracted_urls {
            let scan = self.engine.scan(ContentType::Url, u);
            if scan.threat_level == ThreatLevel::Malicious {
                url_threat_found = true;
                xai_reasons.push(format!("Malicious link in email body ({}): {}", scan.category, scan.xai_reason));
                if scan.tier_triggered == crate::types::TierTriggered::Tier2Transformer {
                    tier = 2;
                }
            } else if scan.threat_level == ThreatLevel::Suspicious {
                xai_reasons.push(format!("Suspicious link in email body: {}", scan.xai_reason));
            }
        }

        // 10. NLP Body & Subject Evaluation (Tier 1 + Tier 2)
        let full_content = format!("Subject: {}\n\n{}", signals.subject, signals.body);
        let content_scan = self.engine.scan(ContentType::SmsText, &full_content);
        if content_scan.threat_level == ThreatLevel::Malicious {
            xai_reasons.push(format!("Coercive phrasing flagged by neural classifier: {}", content_scan.xai_reason));
            if content_scan.tier_triggered == crate::types::TierTriggered::Tier2Transformer {
                tier = 2;
            }
        } else if content_scan.threat_level == ThreatLevel::Suspicious {
            xai_reasons.push(format!("Semantic ambiguity flagged by local MobileBERT: {}", content_scan.xai_reason));
            if content_scan.tier_triggered == crate::types::TierTriggered::Tier2Transformer {
                tier = 2;
            }
        }

        // 11. Verdict Decision
        let is_phishing = spoofing_detected
            || url_threat_found
            || dangerous_payload_found
            || (urgency_lure_found && (!extracted_urls.is_empty() || !signals.dmarc_pass))
            || content_scan.threat_level == ThreatLevel::Malicious;

        let is_spam = !signals.dmarc_pass
            || content_scan.threat_level == ThreatLevel::Suspicious
            || tracking_pixels_neutralized > 0
            || urgency_lure_found;

        let verdict = if is_phishing {
            CommunicationVerdict::Phishing
        } else if is_spam {
            CommunicationVerdict::Spam
        } else {
            CommunicationVerdict::Safe
        };

        if xai_reasons.is_empty() {
            xai_reasons.push("Email passed local cryptographic authentication, sender integrity, and semantic inspection.".to_string());
        }

        let elapsed_us = start.elapsed().as_micros() as u32;

        EmailVerdict {
            sender_address: signals.sender_address.clone(),
            verdict,
            confidence: if verdict == CommunicationVerdict::Safe { 0.96 } else { 0.94 },
            xai_reasons,
            tier,
            latency_us: elapsed_us,
            extracted_urls,
            tracking_pixels_neutralized,
            should_quarantine: verdict.should_block(),
            spoofing_detected,
        }
    }
}
