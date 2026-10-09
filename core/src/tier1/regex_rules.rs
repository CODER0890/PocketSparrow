use lazy_static::lazy_static;
use regex::Regex;

pub struct RegexFinding {
    pub is_matched: bool,
    pub category: &'static str,
    pub reason: String,
    pub confidence: f32,
}

lazy_static! {
    // Subdomain deception: e.g. paypal.com.attacker.com or chase.com.account-update.xyz
    static ref RE_SUBDOMAIN_DECEPTION: Regex = Regex::new(
        r"(?i)(paypal\.com|chase\.com|wellsfargo\.com|bankofamerica\.com|apple\.com|amazon\.com|netflix\.com|google\.com|microsoft\.com)\.[a-z0-9\-]+\.[a-z]{2,}"
    ).unwrap();

    // IP address as hostname: e.g. http://192.168.1.1/login or http://45.33.2.1/
    static ref RE_IP_HOST: Regex = Regex::new(
        r"(?i)^https?://(?:[0-9]{1,3}\.){3}[0-9]{1,3}(?::[0-9]+)?(?:/|$)"
    ).unwrap();

    // Embedded userinfo / credential harvesting syntax
    static ref RE_USERINFO: Regex = Regex::new(
        r"(?i)^https?://[a-z0-9_\-\.]+:[^@]+@"
    ).unwrap();

    // Typosquatting brand lookalikes: g00gle, paypa1, amaz0n, netfl1x, app1e
    static ref RE_TYPOSQUAT: Regex = Regex::new(
        r"(?i)(g00gle|paypa1|amaz0n|netfl1x|app1e|ch4se|w3llsfargo)"
    ).unwrap();

    // High-urgency financial smishing triggers
    static ref RE_SMISHING_WIRE: Regex = Regex::new(
        r"(?i)(wire transfer|unusual wire|unauthorized transaction|sent \$\d+|\$\d+[\.,]\d{2} initiated|cancel transaction now)"
    ).unwrap();

    // Account suspension / coercive urgency
    static ref RE_SMISHING_SUSPEND: Regex = Regex::new(
        r"(?i)(account (?:has been |was )?(?:suspended|locked|paused)|verify identity immediately|confirm your password to unlock|avoid legal action)"
    ).unwrap();

    // Delivery parcel redelivery scams
    static ref RE_SMISHING_DELIVERY: Regex = Regex::new(
        r"(?i)(package could not be delivered|incorrect address|update delivery address within \d+ hours|redelivery fee)"
    ).unwrap();
}

pub fn check_url_patterns(url: &str) -> Option<RegexFinding> {
    if RE_SUBDOMAIN_DECEPTION.is_match(url) {
        return Some(RegexFinding {
            is_matched: true,
            category: "SUBDOMAIN_DECEPTION",
            reason: "Deceptive subdomain spoofing: legitimate brand name embedded as subdomain prefix of untrusted domain".to_string(),
            confidence: 0.96,
        });
    }

    if RE_IP_HOST.is_match(url) {
        return Some(RegexFinding {
            is_matched: true,
            category: "IP_ADDRESS_HOST",
            reason: "Raw IP address used as URL host instead of verified domain name".to_string(),
            confidence: 0.92,
        });
    }

    if RE_USERINFO.is_match(url) {
        return Some(RegexFinding {
            is_matched: true,
            category: "EMBEDDED_CREDENTIALS",
            reason: "URL contains embedded authentication userinfo designed to obfuscate destination host".to_string(),
            confidence: 0.95,
        });
    }

    if RE_TYPOSQUAT.is_match(url) {
        return Some(RegexFinding {
            is_matched: true,
            category: "HOMOGRAPH",
            reason: "Typosquatted domain name detected impersonating well-known technology or banking platform".to_string(),
            confidence: 0.94,
        });
    }

    None
}

pub fn check_sms_patterns(text: &str) -> Option<RegexFinding> {
    if RE_SMISHING_WIRE.is_match(text) {
        return Some(RegexFinding {
            is_matched: true,
            category: "URGENT_WIRE_TRANSFER",
            reason: "High-urgency wire transfer lure attempting to induce panic cancellation or credential submission".to_string(),
            confidence: 0.95,
        });
    }

    if RE_SMISHING_SUSPEND.is_match(text) {
        return Some(RegexFinding {
            is_matched: true,
            category: "CREDENTIAL_HARVESTING",
            reason: "Coercive account suspension threat prompting urgent identity or password verification".to_string(),
            confidence: 0.93,
        });
    }

    if RE_SMISHING_DELIVERY.is_match(text) {
        return Some(RegexFinding {
            is_matched: true,
            category: "DELIVERY_SCAM",
            reason: "Postal delivery notification scam prompting address modification or fee payment".to_string(),
            confidence: 0.91,
        });
    }

    None
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_subdomain_deception() {
        let res = check_url_patterns("https://paypal.com.verify-billing-update.xyz/login");
        assert!(res.is_some());
        assert_eq!(res.unwrap().category, "SUBDOMAIN_DECEPTION");
    }

    #[test]
    fn test_ip_host() {
        let res = check_url_patterns("http://192.168.1.100/chase/login.html");
        assert!(res.is_some());
        assert_eq!(res.unwrap().category, "IP_ADDRESS_HOST");
    }

    #[test]
    fn test_sms_wire() {
        let res = check_sms_patterns("BANK ALERT: Unusual wire transfer of $2,450.00 initiated. Cancel transaction now:");
        assert!(res.is_some());
        assert_eq!(res.unwrap().category, "URGENT_WIRE_TRANSFER");
    }
}
