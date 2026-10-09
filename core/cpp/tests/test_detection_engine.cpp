#include "pocket_sparrow.hpp"
#include <iostream>
#include <cassert>
#include <chrono>

using namespace pocket_sparrow;

void test_tier1_latency_and_heuristics() {
    std::cout << "[RUN] Testing Tier 1 heuristics and latency..." << std::endl;
    DetectionEngine engine;

    std::string test_url = "https://g00gle-security-check.cfd/auth/verify?id=9281";
    auto start = std::chrono::steady_clock::now();
    auto res = engine.scan_url(test_url);
    auto elapsed = std::chrono::steady_clock::now() - start;
    auto elapsed_ms = std::chrono::duration_cast<std::chrono::milliseconds>(elapsed).count();

    std::cout << "  - Latency: " << res.latency_us << " µs (" << elapsed_ms << " ms)" << std::endl;
    assert(elapsed_ms < 5 && "Tier 1 must complete in under 5ms");
    assert(res.verdict == Verdict::Malicious);
    assert(res.category == "HOMOGRAPH");
    assert(res.should_block);
    std::cout << "[PASS] Tier 1 heuristics passed!" << std::endl;
}

void test_cyrillic_homoglyphs() {
    std::cout << "[RUN] Testing Cyrillic homoglyphs..." << std::endl;
    DetectionEngine engine;
    // Cyrillic 'р' (UTF-8 0xD1 0x80)
    std::string fake_paypal = "https://\xD1\x80" "aypal.com/account/login";
    auto res = engine.scan_url(fake_paypal);
    assert(res.verdict == Verdict::Malicious);
    assert(res.category == "HOMOGRAPH");
    std::cout << "[PASS] Cyrillic homoglyph detection passed!" << std::endl;
}

void test_subdomain_deception() {
    std::cout << "[RUN] Testing Subdomain deception..." << std::endl;
    DetectionEngine engine;
    std::string fake_chase = "https://chase.com.security-alert-center.xyz/login";
    auto res = engine.scan_url(fake_chase);
    assert(res.verdict == Verdict::Malicious);
    assert(res.category == "SUBDOMAIN_DECEPTION");
    std::cout << "[PASS] Subdomain deception passed!" << std::endl;
}

void test_qr_quishing_extraction() {
    std::cout << "[RUN] Testing QR quishing extraction..." << std::endl;
    DetectionEngine engine;

    // 1. Wrapped URL in QR
    std::string qr_payload = "MEBKM:TITLE:Bank;URL:https://chase-login.top/auth;;";
    auto res = engine.scan_qr(qr_payload);
    assert(res.verdict == Verdict::Suspicious || res.verdict == Verdict::Malicious);

    // 2. Executable javascript in QR
    std::string evil_qr = "javascript:alert(document.cookie)";
    auto res2 = engine.scan_qr(evil_qr);
    assert(res2.verdict == Verdict::Malicious);
    assert(res2.category == "MALICIOUS_QR_SCHEME");
    std::cout << "[PASS] QR code quishing extraction passed!" << std::endl;
}

void test_apk_permission_scoring() {
    std::cout << "[RUN] Testing APK permission risk scoring..." << std::endl;
    DetectionEngine engine;

    // Banking Trojan cluster
    std::vector<std::string> trojan_perms = {
        "android.permission.RECEIVE_SMS",
        "android.permission.INTERNET",
        "android.permission.SYSTEM_ALERT_WINDOW"
    };
    auto audit = engine.audit_apk_permissions(trojan_perms);
    assert(audit.verdict == Verdict::Malicious);
    assert(audit.risk_score >= 95);
    assert(audit.category == "ROGUE_BANKING_TROJAN");
    assert(audit.should_block);

    // Clean app
    std::vector<std::string> clean_perms = {
        "android.permission.INTERNET",
        "android.permission.ACCESS_NETWORK_STATE"
    };
    auto clean_audit = engine.audit_apk_permissions(clean_perms);
    assert(clean_audit.verdict == Verdict::Safe);
    assert(!clean_audit.should_block);
    std::cout << "[PASS] APK permission risk scoring passed!" << std::endl;
}

void test_two_tier_pipeline_latency() {
    std::cout << "[RUN] Testing total two-tier latency (< 50ms SLA)..." << std::endl;
    DetectionEngine engine;
    std::string sms = "URGENT: Your Chase account ending in 4102 has been suspended. Verify immediately: https://chase-bank-verify.top";

    auto start = std::chrono::steady_clock::now();
    auto res = engine.scan_sms(sms);
    auto elapsed = std::chrono::steady_clock::now() - start;
    auto elapsed_ms = std::chrono::duration_cast<std::chrono::milliseconds>(elapsed).count();

    std::cout << "  - Two-tier latency: " << res.latency_us << " µs (" << elapsed_ms << " ms)" << std::endl;
    assert(elapsed_ms < 50 && "Total pipeline must execute under 50ms");
    assert(res.verdict == Verdict::Malicious);
    assert(!res.xai_reason.empty());
    std::cout << "[PASS] Two-tier latency SLA verified!" << std::endl;
}

int main() {
    std::cout << "================================================================" << std::endl;
    std::cout << "Pocket Sparrow - C++ Detection Engine Test Suite" << std::endl;
    std::cout << "================================================================" << std::endl;

    test_tier1_latency_and_heuristics();
    test_cyrillic_homoglyphs();
    test_subdomain_deception();
    test_qr_quishing_extraction();
    test_apk_permission_scoring();
    test_two_tier_pipeline_latency();

    std::cout << "================================================================" << std::endl;
    std::cout << "[ALL TESTS PASSED] C++ Detection Engine fully verified!" << std::endl;
    std::cout << "================================================================" << std::endl;
    return 0;
}
