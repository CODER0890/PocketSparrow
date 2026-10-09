use crate::types::{ThreatLevel, TierTriggered};

pub struct XaiExplanation {
    pub title: String,
    pub plain_english_summary: String,
    pub technical_detail: String,
    pub recommended_action: String,
}

pub fn generate_xai_card(
    threat_level: ThreatLevel,
    tier: TierTriggered,
    category: &str,
    raw_reason: &str,
    confidence: f32,
) -> XaiExplanation {
    let conf_pct = (confidence * 100.0).round() as u32;

    match threat_level {
        ThreatLevel::Malicious => {
            let (title, action) = match category {
                "HOMOGRAPH" => (
                    "Lookalike Deceptive Domain Detected",
                    "Do not open this link. The domain disguises foreign characters to look identical to a trusted website.",
                ),
                "SUBDOMAIN_DECEPTION" => (
                    "Brand Impersonation in Subdomain",
                    "Do not enter credentials. The brand name is fake and part of a different external website.",
                ),
                "URGENT_WIRE_TRANSFER" => (
                    "Urgent Financial Wire Transfer Scam",
                    "Do not click or call numbers in this message. Banks never require urgent wire cancellations via SMS links.",
                ),
                "CREDENTIAL_HARVESTING" => (
                    "Account Suspension Phishing Lure",
                    "Do not supply passwords or 2FA codes. Legitimate providers do not suspend accounts with urgent external links.",
                ),
                "DELIVERY_SCAM" => (
                    "Fake Package Delivery Redirection",
                    "Do not pay redelivery fees or input payment cards. Check official courier apps directly.",
                ),
                "MALICIOUS_SCHEME" => (
                    "Executable Script or Dangerous Data URI",
                    "Blocked immediately. This link contains executable code designed to compromise your browser.",
                ),
                "ROGUE_BANKING_TROJAN" => (
                    "Malicious Sideloaded APK Detected",
                    "Uninstall immediately. This app requests permissions to intercept your SMS two-factor passcodes and draw over banking apps.",
                ),
                "ROGUE_ACCESSIBILITY_HIJACK" => (
                    "Dangerous Accessibility Exploitation APK",
                    "Uninstall immediately. This app can log keystrokes and take over your device screen.",
                ),
                _ => (
                    "Malicious Threat Blocked",
                    "High-confidence threat identified. Immediate blocking recommended.",
                ),
            };

            XaiExplanation {
                title: title.to_string(),
                plain_english_summary: format!(
                    "{} (Engine confidence: {}%)",
                    action, conf_pct
                ),
                technical_detail: format!(
                    "Detected by {}: {}. Threat Category: {}.",
                    match tier {
                        TierTriggered::Tier1Heuristic => "Tier 1 Heuristic Engine (<5ms)",
                        TierTriggered::Tier2Transformer => "Tier 2 INT8 Transformer NLP (<40ms)",
                    },
                    raw_reason,
                    category
                ),
                recommended_action: action.to_string(),
            }
        }
        ThreatLevel::Suspicious => XaiExplanation {
            title: "Potential Threat / Ambiguous Context".to_string(),
            plain_english_summary: format!(
                "This content exhibits suspicious markers. Proceed with caution. (Confidence: {}%)",
                conf_pct
            ),
            technical_detail: format!(
                "Triggered Tier 2 transformer verification: {}",
                raw_reason
            ),
            recommended_action: "Verify the sender through a known trusted channel before interacting."
                .to_string(),
        },
        ThreatLevel::Safe => XaiExplanation {
            title: "Verified Safe Content".to_string(),
            plain_english_summary: "No indicators of phishing, spoofing, or malicious scam intent were detected.".to_string(),
            technical_detail: format!("Passed all Tier 1 and Tier 2 on-device checks: {}", raw_reason),
            recommended_action: "Content is safe to view.".to_string(),
        },
    }
}
