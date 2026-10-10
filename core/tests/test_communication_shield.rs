use pocket_sparrow::communication_shield::CommunicationShield;
use pocket_sparrow::engine::DetectionEngine;
use pocket_sparrow::types::{
    CallSignals, CommunicationVerdict, EmailSignals, SmsSignals, StirShakenStatus,
};
use std::sync::Arc;
use std::time::Instant;

fn setup_shield() -> CommunicationShield {
    let engine = Arc::new(DetectionEngine::new(None, None));
    CommunicationShield::new(engine)
}

#[test]
fn test_call_screening_safe_contact() {
    let shield = setup_shield();
    let signals = CallSignals {
        phone_number: "+1-555-0100".to_string(),
        contact_match: true,
        stir_shaken_status: StirShakenStatus::Verified,
        local_reputation_score: 1.0,
        call_frequency: 1,
    };

    let verdict = shield.evaluate_call(&signals);
    assert_eq!(verdict.verdict, CommunicationVerdict::Safe);
    assert!(!verdict.should_block);
    assert!(verdict.confidence >= 0.95);
    assert!(verdict.latency_us < 50_000, "Latency must be < 50ms");
}

#[test]
fn test_call_screening_local_blocklist() {
    let shield = setup_shield();
    let signals = CallSignals {
        phone_number: "+1-555-0199".to_string(),
        contact_match: false,
        stir_shaken_status: StirShakenStatus::NotAvailable,
        local_reputation_score: -1.0,
        call_frequency: 1,
    };

    let verdict = shield.evaluate_call(&signals);
    assert_eq!(verdict.verdict, CommunicationVerdict::Spam);
    assert!(verdict.should_block);
    assert!(verdict.xai_reasons.iter().any(|r| r.contains("blocked in local encrypted spam database")));
}

#[test]
fn test_call_screening_stir_shaken_failed_toll_free() {
    let shield = setup_shield();
    let signals = CallSignals {
        phone_number: "18005550000".to_string(),
        contact_match: false,
        stir_shaken_status: StirShakenStatus::Failed,
        local_reputation_score: 0.0,
        call_frequency: 4,
    };

    let verdict = shield.evaluate_call(&signals);
    assert_eq!(verdict.verdict, CommunicationVerdict::Spam);
    assert!(verdict.should_block);
    assert!(verdict.xai_reasons.iter().any(|r| r.contains("STIR/SHAKEN")));
}

#[test]
fn test_sms_screening_smishing_phishing() {
    let shield = setup_shield();
    let signals = SmsSignals {
        sender: "+1-555-0144".to_string(),
        body: "BANK ALERT: Unauthorized wire transfer initiated. Cancel transaction: https://paypal.com.security-alert-center.xyz/login".to_string(),
        local_reputation_score: 0.0,
        is_contact: false,
        campaign_repetition_count: 1,
    };

    let verdict = shield.evaluate_sms(&signals);
    assert_eq!(verdict.verdict, CommunicationVerdict::Phishing);
    assert!(verdict.should_quarantine);
    assert!(!verdict.extracted_urls.is_empty());
}

#[test]
fn test_sms_screening_campaign_detection() {
    let shield = setup_shield();
    let signals = SmsSignals {
        sender: "+1-555-0188".to_string(),
        body: "Claim your complimentary gift card package now: http://example.test/claim".to_string(),
        local_reputation_score: 0.0,
        is_contact: false,
        campaign_repetition_count: 5,
    };

    let verdict = shield.evaluate_sms(&signals);
    assert!(verdict.is_campaign);
    assert!(verdict.should_quarantine);
    assert!(verdict.xai_reasons.iter().any(|r| r.contains("campaign")));
}

#[test]
fn test_email_screening_display_name_spoofing() {
    let shield = setup_shield();
    let signals = EmailSignals {
        sender_address: "billing-fraud@untrusted-domain.xyz".to_string(),
        display_name: Some("PayPal Security Department".to_string()),
        subject: "Action Required: Account Suspended".to_string(),
        body: "Please confirm your login details immediately.".to_string(),
        spf_pass: false,
        dkim_pass: false,
        dmarc_pass: false,
        local_reputation_score: 0.0,
    };

    let verdict = shield.evaluate_email(&signals);
    assert!(verdict.spoofing_detected);
    assert_eq!(verdict.verdict, CommunicationVerdict::Phishing);
    assert!(verdict.should_quarantine);
    assert!(verdict.xai_reasons.iter().any(|r| r.contains("Display-name impersonation")));
}

#[test]
fn test_email_screening_tracking_pixel_neutralization() {
    let shield = setup_shield();
    let signals = EmailSignals {
        sender_address: "newsletter@example.test".to_string(),
        display_name: Some("Tech Newsletter".to_string()),
        subject: "Weekly Digest".to_string(),
        body: "Hello reader! <img src=\"http://example.test/track.gif\" width=\"1\" height=\"1\" /> Welcome to our newsletter.".to_string(),
        spf_pass: true,
        dkim_pass: true,
        dmarc_pass: true,
        local_reputation_score: 0.0,
    };

    let verdict = shield.evaluate_email(&signals);
    assert!(verdict.tracking_pixels_neutralized > 0);
    assert!(verdict.xai_reasons.iter().any(|r| r.contains("Tracking pixel pattern detected")));
}

#[test]
fn test_communication_shield_latency_sla_under_50ms() {
    let shield = setup_shield();
    let call_sig = CallSignals {
        phone_number: "+1-555-0155".to_string(),
        contact_match: false,
        stir_shaken_status: StirShakenStatus::Partial,
        local_reputation_score: 0.0,
        call_frequency: 1,
    };
    let sms_sig = SmsSignals {
        sender: "+1-555-0155".to_string(),
        body: "Meeting reminder for 2 PM tomorrow.".to_string(),
        local_reputation_score: 0.0,
        is_contact: true,
        campaign_repetition_count: 0,
    };
    let email_sig = EmailSignals {
        sender_address: "colleague@example.test".to_string(),
        display_name: Some("Colleague".to_string()),
        subject: "Q3 Planning".to_string(),
        body: "Attached is the roadmap for next quarter.".to_string(),
        spf_pass: true,
        dkim_pass: true,
        dmarc_pass: true,
        local_reputation_score: 0.5,
    };

    let start = Instant::now();
    let cv = shield.evaluate_call(&call_sig);
    let sv = shield.evaluate_sms(&sms_sig);
    let ev = shield.evaluate_email(&email_sig);
    let elapsed = start.elapsed();

    println!("Total tri-shield evaluation latency: {:?}", elapsed);
    assert!(cv.latency_us < 50_000, "Call evaluation must be < 50ms");
    assert!(sv.latency_us < 50_000, "SMS evaluation must be < 50ms");
    assert!(ev.latency_us < 50_000, "Email evaluation must be < 50ms");
    assert!(elapsed.as_millis() < 50, "Combined 3-channel evaluation must be < 50ms");
}
