use pocket_sparrow::engine::DetectionEngine;
use pocket_sparrow::types::ContentType;
use std::time::Instant;

fn main() {
    println!("================================================================");
    println!("Pocket Sparrow - Two-Tier Latency Benchmark Harness");
    println!("Target: Tier 1 < 5ms | Tier 2 < 40ms | Total < 50ms");
    println!("================================================================");

    let engine = DetectionEngine::new(None, None);

    let test_samples = vec![
        (ContentType::Url, "https://g00gle-security-check.cfd/auth/verify?id=9281"),
        (ContentType::Url, "https://chase.com.security-alert-center.xyz/login"),
        (ContentType::Url, "https://\u{0440}aypal.com/account/login"),
        (ContentType::Url, "https://en.wikipedia.org/wiki/Computer_security"),
        (ContentType::SmsText, "BANK ALERT: Unusual wire transfer of $2,450.00 initiated. Cancel now: http://x.top"),
        (ContentType::SmsText, "Your appointment with Dr. Patel is confirmed for tomorrow at 10:30 AM."),
        (ContentType::SmsText, "Your temporary verification passcode and login pin is 48201. Please confirm."),
        (ContentType::QrPayload, "javascript:alert(document.cookie)"),
    ];

    let iterations = 1000;
    let mut latencies_us = Vec::with_capacity(iterations);

    // Warm-up run
    for (ctype, payload) in &test_samples {
        let _ = engine.scan(*ctype, payload);
    }

    let global_start = Instant::now();
    for i in 0..iterations {
        let (ctype, payload) = test_samples[i % test_samples.len()];
        let res = engine.scan(ctype, payload);
        latencies_us.push(res.latency_us);
    }
    let total_elapsed = global_start.elapsed();

    latencies_us.sort_unstable();

    let p50 = latencies_us[iterations * 50 / 100];
    let p95 = latencies_us[iterations * 95 / 100];
    let p99 = latencies_us[iterations * 99 / 100];
    let max = latencies_us[iterations - 1];
    let min = latencies_us[0];
    let avg = latencies_us.iter().sum::<u32>() as f64 / iterations as f64;

    println!("\nBenchmark Results over {} iterations:", iterations);
    println!("----------------------------------------------------------------");
    println!("Total Elapsed Time: {:?} ({:.2} scans/sec)", total_elapsed, iterations as f64 / total_elapsed.as_secs_f64());
    println!("Min Latency:        {} µs ({:.3} ms)", min, min as f64 / 1000.0);
    println!("Average Latency:    {:.1} µs ({:.3} ms)", avg, avg / 1000.0);
    println!("p50 Latency:        {} µs ({:.3} ms)", p50, p50 as f64 / 1000.0);
    println!("p95 Latency:        {} µs ({:.3} ms)", p95, p95 as f64 / 1000.0);
    println!("p99 Latency:        {} µs ({:.3} ms)", p99, p99 as f64 / 1000.0);
    println!("Max Latency:        {} µs ({:.3} ms)", max, max as f64 / 1000.0);
    println!("----------------------------------------------------------------");

    if p99 < 50_000 {
        println!("[PASS] Latency SLA Met: p99 latency ({:.2} ms) is well within < 50ms ceiling!", p99 as f64 / 1000.0);
    } else {
        println!("[FAIL] Latency SLA Violated: p99 exceeds 50ms.");
        std::process::exit(1);
    }
}
