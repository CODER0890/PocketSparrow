use pocket_sparrow::tier1::HeuristicsEngine;
use pocket_sparrow::types::{ContentType, ThreatLevel};
use std::time::Instant;

#[test]
fn test_tier1_latency_under_5ms() {
    let engine = HeuristicsEngine::new();
    let sample = "https://g00gle-security-check.cfd/auth/verify?id=9281";

    // Warm-up for lazy_static initialization
    let _ = engine.evaluate(ContentType::Url, sample);

    let start = Instant::now();
    let res = engine.evaluate(ContentType::Url, sample);
    let elapsed = start.elapsed();

    println!("Tier 1 warm latency: {:?}", elapsed);
    assert!(elapsed.as_millis() < 5, "Tier 1 must complete in under 5ms");
    assert_eq!(res.threat_level, ThreatLevel::Malicious);
    assert_eq!(res.category, "HOMOGRAPH");
}

#[test]
fn test_cyrillic_homoglyph_detection() {
    let engine = HeuristicsEngine::new();
    // Cyrillic 'р' (\u{0440}) spoofing 'p'
    let fake_paypal = "https://\u{0440}aypal.com/account/login";
    let res = engine.evaluate(ContentType::Url, fake_paypal);

    assert_eq!(res.threat_level, ThreatLevel::Malicious);
    assert_eq!(res.category, "HOMOGRAPH");
    assert!(res.reason.contains("Cyrillic homograph"));
}

#[test]
fn test_subdomain_deception() {
    let engine = HeuristicsEngine::new();
    let fake_chase = "https://chase.com.security-alert-center.xyz/login";
    let res = engine.evaluate(ContentType::Url, fake_chase);

    assert_eq!(res.threat_level, ThreatLevel::Malicious);
    assert_eq!(res.category, "SUBDOMAIN_DECEPTION");
}

#[test]
fn test_urgent_wire_smishing() {
    let engine = HeuristicsEngine::new();
    let sms = "BANK ALERT: Unusual wire transfer of $2,450.00 initiated to unknown recipient. Cancel transaction now: http://fake.com";
    let res = engine.evaluate(ContentType::SmsText, sms);

    assert_eq!(res.threat_level, ThreatLevel::Malicious);
    assert!(res.category == "URGENT_WIRE_TRANSFER" || res.category == "PHISHING");
}

#[test]
fn test_benign_url() {
    let engine = HeuristicsEngine::new();
    let clean = "https://en.wikipedia.org/wiki/Computer_security";
    let res = engine.evaluate(ContentType::Url, clean);

    assert_eq!(res.threat_level, ThreatLevel::Safe);
    assert_eq!(res.category, "SAFE");
}

#[test]
fn test_apk_rogue_trojan() {
    let engine = HeuristicsEngine::new();
    let perms = [
        "android.permission.RECEIVE_SMS",
        "android.permission.INTERNET",
        "android.permission.SYSTEM_ALERT_WINDOW",
    ];
    let res = engine.evaluate_apk_permissions(&perms, Instant::now());

    assert_eq!(res.threat_level, ThreatLevel::Malicious);
    assert_eq!(res.category, "ROGUE_BANKING_TROJAN");
}
