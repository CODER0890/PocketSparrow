use crate::types::{PermissionAuditResult, ThreatLevel};
use std::collections::HashSet;

pub const HIGH_RISK_TLDS: &[&str] = &[
    "top", "xyz", "cfd", "rest", "loan", "club", "shop", "work",
    "click", "gq", "ml", "cf", "tk", "ga", "buzz", "cam", "sbs",
    "quest", "monster", "icu", "cyou", "fit", "surf", "casa"
];

pub struct StaticRuleFinding {
    pub is_matched: bool,
    pub category: &'static str,
    pub reason: String,
    pub confidence: f32,
}

pub fn check_high_risk_tld(url: &str) -> Option<StaticRuleFinding> {
    let clean = url.trim_start_matches("http://").trim_start_matches("https://");
    let host_part = clean.split('/').next().unwrap_or("").split(':').next().unwrap_or("");
    let host_lower = host_part.to_lowercase();

    for &tld in HIGH_RISK_TLDS {
        let suffix = format!(".{}", tld);
        if host_lower.ends_with(&suffix) {
            return Some(StaticRuleFinding {
                is_matched: true,
                category: "HIGH_RISK_TLD",
                reason: format!(
                    "Domain uses high-abuse top-level domain '.{}' frequently exploited by automated disposable phishing clusters",
                    tld
                ),
                confidence: 0.88,
            });
        }
    }

    None
}

pub fn check_dangerous_schemes(payload: &str) -> Option<StaticRuleFinding> {
    let lower = payload.trim().to_lowercase();
    if lower.starts_with("javascript:") {
        return Some(StaticRuleFinding {
            is_matched: true,
            category: "MALICIOUS_SCHEME",
            reason: "Embedded JavaScript execution pseudo-scheme (javascript:) detected in link or QR code".to_string(),
            confidence: 0.99,
        });
    }

    if lower.starts_with("data:text/html") || lower.starts_with("data:application/") {
        return Some(StaticRuleFinding {
            is_matched: true,
            category: "MALICIOUS_SCHEME",
            reason: "Data URI payload detected attempting on-the-fly HTML execution or file drop".to_string(),
            confidence: 0.98,
        });
    }

    None
}

/// Computes granular risk score (0 to 100) and identifies dangerous permission clusters.
pub fn audit_permission_matrix(permissions: &[&str]) -> PermissionAuditResult {
    let perm_set: HashSet<String> = permissions.iter().map(|p| p.trim().to_string()).collect();

    let has_sms_read = perm_set.contains("android.permission.RECEIVE_SMS") 
        || perm_set.contains("android.permission.READ_SMS");
    let has_internet = perm_set.contains("android.permission.INTERNET");
    let has_overlay = perm_set.contains("android.permission.SYSTEM_ALERT_WINDOW");
    let has_accessibility = perm_set.contains("android.permission.BIND_ACCESSIBILITY_SERVICE");
    let has_install = perm_set.contains("android.permission.REQUEST_INSTALL_PACKAGES");
    let has_contacts = perm_set.contains("android.permission.READ_CONTACTS");
    let has_audio = perm_set.contains("android.permission.RECORD_AUDIO");
    let has_location_bg = perm_set.contains("android.permission.ACCESS_BACKGROUND_LOCATION");
    let has_boot = perm_set.contains("android.permission.RECEIVE_BOOT_COMPLETED");

    let mut flagged = Vec::new();
    let mut base_score: u32 = 0;

    for p in &perm_set {
        let weight = match p.as_str() {
            "android.permission.BIND_ACCESSIBILITY_SERVICE" => 35,
            "android.permission.SYSTEM_ALERT_WINDOW" => 25,
            "android.permission.RECEIVE_SMS" | "android.permission.READ_SMS" => 25,
            "android.permission.REQUEST_INSTALL_PACKAGES" => 20,
            "android.permission.ACCESS_BACKGROUND_LOCATION" => 20,
            "android.permission.RECORD_AUDIO" => 15,
            "android.permission.READ_CALL_LOG" => 15,
            "android.permission.READ_CONTACTS" => 10,
            "android.permission.CAMERA" => 10,
            "android.permission.INTERNET" => 5,
            _ => 2,
        };
        base_score += weight;
        if weight >= 15 {
            flagged.push(p.clone());
        }
    }

    // Critical Threat Cluster 1: Banking Trojan / OTP Stealer
    if has_sms_read && has_internet && has_overlay {
        return PermissionAuditResult {
            risk_score: 98,
            threat_level: ThreatLevel::Malicious,
            category: "ROGUE_BANKING_TROJAN".to_string(),
            flagged_permissions: vec![
                "android.permission.RECEIVE_SMS".to_string(),
                "android.permission.INTERNET".to_string(),
                "android.permission.SYSTEM_ALERT_WINDOW".to_string(),
            ],
            explanation: "Critical Banking Trojan profile: Can intercept 2FA SMS tokens and draw deceptive overlays over banking applications.".to_string(),
            should_block: true,
        };
    }

    // Critical Threat Cluster 2: Full Accessibility Hijack & Keylogger
    if has_accessibility && has_overlay {
        return PermissionAuditResult {
            risk_score: 97,
            threat_level: ThreatLevel::Malicious,
            category: "ROGUE_ACCESSIBILITY_HIJACK".to_string(),
            flagged_permissions: vec![
                "android.permission.BIND_ACCESSIBILITY_SERVICE".to_string(),
                "android.permission.SYSTEM_ALERT_WINDOW".to_string(),
            ],
            explanation: "Critical UI Takeover profile: Grants accessibility service control and system window overlays for screen scraping and simulated user taps.".to_string(),
            should_block: true,
        };
    }

    // Critical Threat Cluster 3: Silent Payload Dropper
    if has_install && has_internet {
        return PermissionAuditResult {
            risk_score: 85,
            threat_level: ThreatLevel::Malicious,
            category: "ROGUE_DROPPER".to_string(),
            flagged_permissions: vec![
                "android.permission.REQUEST_INSTALL_PACKAGES".to_string(),
                "android.permission.INTERNET".to_string(),
            ],
            explanation: "High-risk Dropper behavior: Allows downloading and triggering background installation of unauthorized APK packages.".to_string(),
            should_block: true,
        };
    }

    // Critical Threat Cluster 4: Audio/Contact Spyware
    if has_audio && has_contacts && has_internet {
        return PermissionAuditResult {
            risk_score: 89,
            threat_level: ThreatLevel::Malicious,
            category: "ROGUE_SPYWARE".to_string(),
            flagged_permissions: vec![
                "android.permission.RECORD_AUDIO".to_string(),
                "android.permission.READ_CONTACTS".to_string(),
                "android.permission.INTERNET".to_string(),
            ],
            explanation: "Spyware surveillance profile: Unrestricted ambient audio recording coupled with address book exfiltration.".to_string(),
            should_block: true,
        };
    }

    // Moderate Risk Cluster: Background Tracking
    if has_location_bg && has_boot {
        return PermissionAuditResult {
            risk_score: 75,
            threat_level: ThreatLevel::Suspicious,
            category: "SUSPICIOUS_TRACKER".to_string(),
            flagged_permissions: vec![
                "android.permission.ACCESS_BACKGROUND_LOCATION".to_string(),
                "android.permission.RECEIVE_BOOT_COMPLETED".to_string(),
            ],
            explanation: "Persistent location tracking: App requests background location updates beginning at device boot.".to_string(),
            should_block: false,
        };
    }

    // Safe / low-risk app
    let final_score = base_score.min(45);
    PermissionAuditResult {
        risk_score: final_score,
        threat_level: if final_score > 35 { ThreatLevel::Suspicious } else { ThreatLevel::Safe },
        category: "STANDARD_PERMISSIONS".to_string(),
        flagged_permissions: flagged,
        explanation: "No high-risk malicious permission combinations detected in application manifest.".to_string(),
        should_block: false,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_high_risk_tld() {
        let res = check_high_risk_tld("https://secure-login.cfd/auth");
        assert!(res.is_some());
        assert_eq!(res.unwrap().category, "HIGH_RISK_TLD");
    }

    #[test]
    fn test_benign_tld() {
        let res = check_high_risk_tld("https://wikipedia.org/wiki/Main_Page");
        assert!(res.is_none());
    }

    #[test]
    fn test_banking_trojan_scoring() {
        let perms = [
            "android.permission.RECEIVE_SMS",
            "android.permission.INTERNET",
            "android.permission.SYSTEM_ALERT_WINDOW",
        ];
        let audit = audit_permission_matrix(&perms);
        assert_eq!(audit.threat_level, ThreatLevel::Malicious);
        assert_eq!(audit.risk_score, 98);
        assert_eq!(audit.category, "ROGUE_BANKING_TROJAN");
        assert!(audit.should_block);
    }
}
