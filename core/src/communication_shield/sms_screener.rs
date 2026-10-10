use crate::engine::DetectionEngine;
use crate::types::{CommunicationVerdict, ContentType, SmsSignals, SmsVerdict, ThreatLevel};
use std::sync::Arc;
use std::time::Instant;

pub struct SmsScreener {
    engine: Arc<DetectionEngine>,
}

impl SmsScreener {
    pub fn new(engine: Arc<DetectionEngine>) -> Self {
        Self { engine }
    }

    /// Evaluates incoming SMS/RCS message on-device in < 25ms.
    pub fn screen_sms(&self, signals: &SmsSignals) -> SmsVerdict {
        let start = Instant::now();
        let mut xai_reasons = Vec::new();
        let mut tier: u8 = 1;
        let mut is_campaign = false;

        // 1. Extract embedded URLs from SMS body
        let mut extracted_urls = Vec::new();
        for word in signals.body.split_whitespace() {
            if word.starts_with("http://") || word.starts_with("https://") {
                let clean = word.trim_matches(|c| c == '<' || c == '>' || c == '"' || c == '\'' || c == ',' || c == '.');
                extracted_urls.push(clean.to_string());
            }
        }

        // 2. Local Reputation Blocklist
        if signals.local_reputation_score <= -0.5 {
            xai_reasons.push("Sender is marked as blocked in local encrypted spam database.".to_string());
            let elapsed_us = start.elapsed().as_micros() as u32;
            return SmsVerdict {
                sender: signals.sender.clone(),
                verdict: CommunicationVerdict::Spam,
                confidence: 0.98,
                xai_reasons,
                tier: 1,
                latency_us: elapsed_us,
                extracted_urls,
                should_quarantine: true,
                is_campaign: false,
            };
        }

        // 3. Scan Embedded URLs through Tier 1 + Tier 2
        let mut url_phishing_found = false;
        for url in &extracted_urls {
            let scan = self.engine.scan(ContentType::Url, url);
            if scan.threat_level == ThreatLevel::Malicious {
                url_phishing_found = true;
                xai_reasons.push(format!("Malicious link detected ({}): {}", scan.category, scan.xai_reason));
                if scan.tier_triggered == crate::types::TierTriggered::Tier2Transformer {
                    tier = 2;
                }
            } else if scan.threat_level == ThreatLevel::Suspicious {
                xai_reasons.push(format!("Suspicious link detected ({}): {}", scan.category, scan.xai_reason));
            }
        }

        if url_phishing_found {
            let elapsed_us = start.elapsed().as_micros() as u32;
            return SmsVerdict {
                sender: signals.sender.clone(),
                verdict: CommunicationVerdict::Phishing,
                confidence: 0.96,
                xai_reasons,
                tier,
                latency_us: elapsed_us,
                extracted_urls,
                should_quarantine: true,
                is_campaign: false,
            };
        }

        // 4. Scan Body text for Coercion / Smishing NLP (Tier 1 + Tier 2)
        let body_scan = self.engine.scan(ContentType::SmsText, &signals.body);
        if body_scan.threat_level == ThreatLevel::Malicious {
            xai_reasons.push(format!("Coercive urgency detected ({}): {}", body_scan.category, body_scan.xai_reason));
            if body_scan.tier_triggered == crate::types::TierTriggered::Tier2Transformer {
                tier = 2;
            }
        } else if body_scan.threat_level == ThreatLevel::Suspicious {
            xai_reasons.push(format!("Potential social engineering detected: {}", body_scan.xai_reason));
            if body_scan.tier_triggered == crate::types::TierTriggered::Tier2Transformer {
                tier = 2;
            }
        }

        // 5. Campaign Repetition Detection
        if signals.campaign_repetition_count >= 3 {
            is_campaign = true;
            xai_reasons.push(format!(
                "Coordinated spam campaign pattern detected ({} identical message templates recorded on device).",
                signals.campaign_repetition_count
            ));
        }

        // 6. Whitelist Contact bypass if no threats flagged
        if signals.is_contact && body_scan.threat_level == ThreatLevel::Safe && extracted_urls.is_empty() {
            xai_reasons.clear();
            xai_reasons.push("Sender in local address book contacts (Verified Safe).".to_string());
            let elapsed_us = start.elapsed().as_micros() as u32;
            return SmsVerdict {
                sender: signals.sender.clone(),
                verdict: CommunicationVerdict::Safe,
                confidence: 0.99,
                xai_reasons,
                tier: 1,
                latency_us: elapsed_us,
                extracted_urls,
                should_quarantine: false,
                is_campaign: false,
            };
        }

        // Synthesize verdict
        let verdict = if body_scan.threat_level == ThreatLevel::Malicious {
            CommunicationVerdict::Phishing
        } else if is_campaign || body_scan.threat_level == ThreatLevel::Suspicious {
            CommunicationVerdict::Spam
        } else {
            CommunicationVerdict::Safe
        };

        if xai_reasons.is_empty() {
            xai_reasons.push("Message evaluated safe without coercive patterns or untrusted links.".to_string());
        }

        let elapsed_us = start.elapsed().as_micros() as u32;

        SmsVerdict {
            sender: signals.sender.clone(),
            verdict,
            confidence: body_scan.confidence.max(0.75),
            xai_reasons,
            tier,
            latency_us: elapsed_us,
            extracted_urls,
            should_quarantine: verdict.should_block(),
            is_campaign,
        }
    }
}
