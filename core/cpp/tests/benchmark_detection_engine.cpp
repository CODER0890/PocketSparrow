#include "pocket_sparrow.hpp"
#include <iostream>
#include <vector>
#include <numeric>
#include <algorithm>
#include <chrono>

using namespace pocket_sparrow;

int main() {
    std::cout << "================================================================" << std::endl;
    std::cout << "Pocket Sparrow - C++ Two-Tier Latency Benchmark Harness" << std::endl;
    std::cout << "Target: Tier 1 < 5ms | Tier 2 < 40ms | Total < 50ms" << std::endl;
    std::cout << "================================================================" << std::endl;

    DetectionEngine engine;

    std::vector<std::pair<ContentType, std::string>> test_vectors = {
        {ContentType::Url, "https://g00gle-security-check.cfd/auth/verify?id=9281"},
        {ContentType::Url, "https://chase.com.security-alert-center.xyz/login"},
        {ContentType::Url, "https://\xD1\x80" "aypal.com/account/login"},
        {ContentType::Url, "https://en.wikipedia.org/wiki/Computer_security"},
        {ContentType::SmsText, "BANK ALERT: Unusual wire transfer of $2,450.00 initiated. Cancel now: http://x.top"},
        {ContentType::SmsText, "Your appointment with Dr. Patel is confirmed for tomorrow at 10:30 AM."},
        {ContentType::SmsText, "Your temporary verification passcode and login pin is 48201. Please confirm."},
        {ContentType::QrPayload, "MEBKM:TITLE:Bank;URL:https://chase-login.top/auth;;"},
        {ContentType::QrPayload, "javascript:alert(document.cookie)"}
    };

    // Warm-up pass
    for (const auto& item : test_vectors) {
        engine.scan(item.first, item.second);
    }

    const size_t iterations = 1000;
    std::vector<uint32_t> latencies_us;
    latencies_us.reserve(iterations);

    auto global_start = std::chrono::steady_clock::now();
    for (size_t i = 0; i < iterations; ++i) {
        const auto& item = test_vectors[i % test_vectors.size()];
        auto res = engine.scan(item.first, item.second);
        latencies_us.push_back(res.latency_us);
    }
    auto global_elapsed = std::chrono::steady_clock::now() - global_start;
    double elapsed_ms = std::chrono::duration_cast<std::chrono::duration<double, std::milli>>(global_elapsed).count();

    std::sort(latencies_us.begin(), latencies_us.end());

    uint32_t min_lat = latencies_us.front();
    uint32_t max_lat = latencies_us.back();
    uint32_t p50 = latencies_us[iterations * 50 / 100];
    uint32_t p95 = latencies_us[iterations * 95 / 100];
    uint32_t p99 = latencies_us[iterations * 99 / 100];
    double avg = std::accumulate(latencies_us.begin(), latencies_us.end(), 0.0) / static_cast<double>(iterations);

    std::cout << "\nBenchmark Results over " << iterations << " iterations:" << std::endl;
    std::cout << "----------------------------------------------------------------" << std::endl;
    std::cout << "Total Elapsed Time: " << elapsed_ms << " ms (" << (iterations / (elapsed_ms / 1000.0)) << " scans/sec)" << std::endl;
    std::cout << "Min Latency:        " << min_lat << " µs (" << (min_lat / 1000.0) << " ms)" << std::endl;
    std::cout << "Average Latency:    " << avg << " µs (" << (avg / 1000.0) << " ms)" << std::endl;
    std::cout << "p50 Latency:        " << p50 << " µs (" << (p50 / 1000.0) << " ms)" << std::endl;
    std::cout << "p95 Latency:        " << p95 << " µs (" << (p95 / 1000.0) << " ms)" << std::endl;
    std::cout << "p99 Latency:        " << p99 << " µs (" << (p99 / 1000.0) << " ms)" << std::endl;
    std::cout << "Max Latency:        " << max_lat << " µs (" << (max_lat / 1000.0) << " ms)" << std::endl;
    std::cout << "----------------------------------------------------------------" << std::endl;

    if (p99 < 50000) {
        std::cout << "[PASS] Latency SLA Met: p99 latency (" << (p99 / 1000.0) << " ms) is well within < 50ms ceiling!" << std::endl;
        return 0;
    } else {
        std::cerr << "[FAIL] Latency SLA Violated: p99 exceeds 50ms." << std::endl;
        return 1;
    }
}
