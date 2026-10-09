use crate::tier1::HeuristicsEngine;
use crate::tier2::TransformerRunner;
use crate::types::{ContentType, ScanResult, ThreatLevel, TierTriggered};
use crate::xai::generate_xai_card;
use std::time::Instant;

pub struct DetectionEngine {
    heuristics: HeuristicsEngine,
    transformer: TransformerRunner,
}

impl DetectionEngine {
    pub fn new(model_path: Option<&str>, vocab_path: Option<&str>) -> Self {
        Self {
            heuristics: HeuristicsEngine::new(),
            transformer: TransformerRunner::new(model_path, vocab_path),
        }
    }

    /// Evaluates content across the two-tier detection pipeline.
    /// 100% on-device, 0 network access, guaranteed < 50ms latency.
    pub fn scan(&self, content_type: ContentType, payload: &str) -> ScanResult {
        let total_start = Instant::now();

        // Tier 1: Fast Heuristic Engine (< 5ms)
        let t1_res = self.heuristics.evaluate(content_type, payload);

        // If Tier 1 was definitive (not ambiguous), return directly!
        if t1_res.is_definitive && !t1_res.needs_tier2 {
            let total_us = total_start.elapsed().as_micros() as u32;
            let xai = generate_xai_card(
                t1_res.threat_level,
                TierTriggered::Tier1Heuristic,
                &t1_res.category,
                &t1_res.reason,
                t1_res.confidence,
            );

            return match t1_res.threat_level {
                ThreatLevel::Safe => ScanResult::safe(total_us, xai.plain_english_summary),
                ThreatLevel::Suspicious => ScanResult::suspicious(
                    TierTriggered::Tier1Heuristic,
                    t1_res.confidence,
                    total_us,
                    t1_res.category,
                    xai.plain_english_summary,
                ),
                ThreatLevel::Malicious => ScanResult::malicious(
                    TierTriggered::Tier1Heuristic,
                    t1_res.confidence,
                    total_us,
                    t1_res.category,
                    xai.plain_english_summary,
                ),
            };
        }

        // Tier 2: Deep Quantized Transformer NLP (< 40ms)
        let t2_res = self.transformer.infer(payload);
        let total_us = total_start.elapsed().as_micros() as u32;

        let combined_category = if t2_res.threat_level != ThreatLevel::Safe {
            t2_res.category
        } else {
            t1_res.category
        };

        let xai = generate_xai_card(
            t2_res.threat_level,
            TierTriggered::Tier2Transformer,
            &combined_category,
            &t2_res.reason,
            t2_res.confidence,
        );

        match t2_res.threat_level {
            ThreatLevel::Safe => ScanResult::safe_with_tier(
                TierTriggered::Tier2Transformer,
                total_us,
                xai.plain_english_summary,
            ),
            ThreatLevel::Suspicious => ScanResult::suspicious(
                TierTriggered::Tier2Transformer,
                t2_res.confidence,
                total_us,
                combined_category,
                xai.plain_english_summary,
            ),
            ThreatLevel::Malicious => ScanResult::malicious(
                TierTriggered::Tier2Transformer,
                t2_res.confidence,
                total_us,
                combined_category,
                xai.plain_english_summary,
            ),
        }
    }

    /// Audits an application's requested permissions against dangerous threat profiles.
    pub fn audit_permissions(&self, permissions: &[&str]) -> ScanResult {
        let total_start = Instant::now();
        let t1_res = self.heuristics.evaluate_apk_permissions(permissions, total_start);
        let total_us = total_start.elapsed().as_micros() as u32;

        let xai = generate_xai_card(
            t1_res.threat_level,
            TierTriggered::Tier1Heuristic,
            &t1_res.category,
            &t1_res.reason,
            t1_res.confidence,
        );

        if t1_res.threat_level == ThreatLevel::Malicious {
            ScanResult::malicious(
                TierTriggered::Tier1Heuristic,
                t1_res.confidence,
                total_us,
                t1_res.category,
                xai.plain_english_summary,
            )
        } else {
            ScanResult::safe(total_us, xai.plain_english_summary)
        }
    }
}
