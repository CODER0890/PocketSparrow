use pocket_sparrow::engine::DetectionEngine;
use pocket_sparrow::types::{ContentType, ThreatLevel, TierTriggered};
use std::time::Instant;

#[test]
fn test_engine_total_latency_under_50ms() {
    let engine = DetectionEngine::new(None, None);
    let sample = "URGENT: Your Chase account ending in 4102 has been suspended. Verify immediately: https://chase-bank-verify.top";

    let start = Instant::now();
    let res = engine.scan(ContentType::SmsText, sample);
    let elapsed = start.elapsed();

    println!("Total scan latency: {:?}, latency_us: {}", elapsed, res.latency_us);
    assert!(elapsed.as_millis() < 50, "Total pipeline must execute in under 50ms");
    assert_eq!(res.threat_level, ThreatLevel::Malicious);
    assert!(res.should_block);
    assert!(!res.xai_reason.is_empty(), "XAI reason must be populated");
}

#[test]
fn test_engine_tier2_trigger_on_ambiguity() {
    let engine = DetectionEngine::new(None, None);
    // Ambiguous message triggering Tier 2 transformer
    let sample = "Your temporary verification passcode and login pin is 48201. Please confirm.";

    let res = engine.scan(ContentType::SmsText, sample);
    println!("Ambiguous scan result: tier={:?}, level={:?}, reason={}", res.tier_triggered, res.threat_level, res.xai_reason);
    assert_eq!(res.tier_triggered, TierTriggered::Tier2Transformer);
}

#[test]
fn test_engine_c_abi_compatibility() {
    use pocket_sparrow::*;
    use std::ffi::CString;

    unsafe {
        let handle = ps_engine_init(std::ptr::null(), std::ptr::null());
        assert!(!handle.is_null());

        let url = CString::new("https://g00gle-security.cfd/auth").unwrap();
        let result = ps_scan_content(handle, types::ContentType::Url, url.as_ptr());

        assert_eq!(result.threat_level, types::ThreatLevel::Malicious);
        assert!(result.should_block);

        ps_engine_free(handle);
    }
}
