use crate::types::{CallSignals, CallVerdict, CommunicationVerdict, StirShakenStatus};
use std::time::Instant;

pub struct CallScreener;

impl CallScreener {
    pub fn new() -> Self {
        Self
    }

    /// Evaluates an incoming call strictly on-device in < 15ms.
    pub fn screen_call(&self, signals: &CallSignals) -> CallVerdict {
        let start = Instant::now();
        let mut xai_reasons = Vec::new();
        let mut risk_score: f32 = 0.0;
        let tier: u8 = 1;

        // 1. Whitelist Fast Path: Known Contacts are unconditionally SAFE
        if signals.contact_match {
            xai_reasons.push("Caller matches address book contact list (Verified Safe).".to_string());
            let elapsed_us = start.elapsed().as_micros() as u32;
            return CallVerdict {
                phone_number: signals.phone_number.clone(),
                verdict: CommunicationVerdict::Safe,
                confidence: 0.99,
                xai_reasons,
                tier: 1,
                latency_us: elapsed_us,
                should_block: false,
                stir_attested: true,
            };
        }

        // 2. User-reported Local Blocklist Check
        if signals.local_reputation_score <= -0.5 {
            xai_reasons.push("Number is explicitly blocked in local encrypted spam database.".to_string());
            let elapsed_us = start.elapsed().as_micros() as u32;
            return CallVerdict {
                phone_number: signals.phone_number.clone(),
                verdict: CommunicationVerdict::Spam,
                confidence: 0.98,
                xai_reasons,
                tier: 1,
                latency_us: elapsed_us,
                should_block: true,
                stir_attested: false,
            };
        }

        // 3. STIR/SHAKEN Cryptographic Attestation Verification
        match signals.stir_shaken_status {
            StirShakenStatus::Failed => {
                risk_score += 0.55;
                xai_reasons.push("STIR/SHAKEN carrier attestation failed (Cryptographic spoofing detected).".to_string());
            }
            StirShakenStatus::Partial => {
                risk_score += 0.15;
                xai_reasons.push("STIR/SHAKEN partial attestation (Carrier could not confirm origination source).".to_string());
            }
            StirShakenStatus::Verified => {
                risk_score -= 0.35;
                xai_reasons.push("STIR/SHAKEN carrier cryptographic signature verified (Origin authentic).".to_string());
            }
            StirShakenStatus::NotAvailable => {
                // Neutral baseline
            }
        }

        // 4. Number Structure & Pattern Anomalies
        let clean_num: String = signals.phone_number.chars().filter(|c| c.is_ascii_digit()).collect();
        
        // Toll-Free high-risk telemarketing prefixes (800, 888, 877, 866, 855, 844, 833)
        let is_toll_free = clean_num.starts_with("1800")
            || clean_num.starts_with("1888")
            || clean_num.starts_with("1877")
            || clean_num.starts_with("1866")
            || clean_num.starts_with("1855")
            || clean_num.starts_with("1844")
            || clean_num.starts_with("1833")
            || clean_num.starts_with("800")
            || clean_num.starts_with("888");

        if is_toll_free {
            risk_score += 0.25;
            xai_reasons.push("Toll-free commercial prefix detected commonly associated with telemarketing.".to_string());
        }

        // Sequential or repeating patterns often found in automated dialers (e.g. 555-0000, 111-1111)
        if clean_num.len() >= 7 {
            let last_4 = &clean_num[clean_num.len() - 4..];
            if last_4.chars().all(|c| c == '0') || last_4.chars().all(|c| c == last_4.chars().next().unwrap()) {
                risk_score += 0.30;
                xai_reasons.push("Repeating repetitive digits pattern indicative of automated dialer.".to_string());
            }
        }

        // 5. High-Frequency Bursts
        if signals.call_frequency >= 3 {
            risk_score += 0.35;
            xai_reasons.push(format!("Burst dialer activity: {} repeated calls within tracking window.", signals.call_frequency));
        }

        // Clamp risk score to [0.0, 1.0]
        let final_risk = risk_score.clamp(0.0, 1.0);
        let verdict = if final_risk >= 0.70 {
            CommunicationVerdict::Spam
        } else if final_risk >= 0.40 {
            CommunicationVerdict::Suspicious
        } else {
            CommunicationVerdict::Safe
        };

        if xai_reasons.is_empty() {
            xai_reasons.push("Standard unflagged external caller without heuristic risk markers.".to_string());
        }

        let elapsed_us = start.elapsed().as_micros() as u32;

        CallVerdict {
            phone_number: signals.phone_number.clone(),
            verdict,
            confidence: if verdict == CommunicationVerdict::Safe { 1.0 - final_risk } else { final_risk }.max(0.60),
            xai_reasons,
            tier,
            latency_us: elapsed_us,
            should_block: verdict.should_block(),
            stir_attested: signals.stir_shaken_status == StirShakenStatus::Verified,
        }
    }
}
