use serde::{Deserialize, Serialize};
use std::os::raw::c_char;

#[repr(C)]
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum ThreatLevel {
    Safe = 0,
    Suspicious = 1,
    Malicious = 2,
}

pub type Verdict = ThreatLevel;

impl ThreatLevel {
    pub fn from_i32(val: i32) -> Self {
        match val {
            1 => ThreatLevel::Suspicious,
            2 => ThreatLevel::Malicious,
            _ => ThreatLevel::Safe,
        }
    }

    pub fn to_str(&self) -> &'static str {
        match self {
            ThreatLevel::Safe => "SAFE",
            ThreatLevel::Suspicious => "SUSPICIOUS",
            ThreatLevel::Malicious => "MALICIOUS",
        }
    }
}

#[repr(C)]
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum TierTriggered {
    Tier1Heuristic = 1,
    Tier2Transformer = 2,
}

#[repr(C)]
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum ContentType {
    Url = 0,
    SmsText = 1,
    QrPayload = 2,
    ApkPermissions = 3,
}

impl ContentType {
    pub fn from_i32(val: i32) -> Self {
        match val {
            1 => ContentType::SmsText,
            2 => ContentType::QrPayload,
            3 => ContentType::ApkPermissions,
            _ => ContentType::Url,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct HeuristicResult {
    pub is_definitive: bool,
    pub threat_level: ThreatLevel,
    pub confidence: f32,
    pub category: String,
    pub reason: String,
    pub latency_us: u32,
    pub needs_tier2: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModelResult {
    pub threat_level: ThreatLevel,
    pub confidence: f32,
    pub category: String,
    pub reason: String,
    pub latency_us: u32,
    pub salient_tokens: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct XAIExplanation {
    pub title: String,
    pub plain_english_summary: String,
    pub technical_detail: String,
    pub recommended_action: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PermissionAuditResult {
    pub risk_score: u32, // 0 to 100
    pub threat_level: ThreatLevel,
    pub category: String,
    pub flagged_permissions: Vec<String>,
    pub explanation: String,
    pub should_block: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ScanResult {
    pub threat_level: ThreatLevel,
    pub tier_triggered: TierTriggered,
    pub confidence: f32,
    pub latency_us: u32,
    pub category: String,
    pub xai_reason: String,
    pub should_block: bool,
}

impl ScanResult {
    pub fn safe(latency_us: u32, reason: impl Into<String>) -> Self {
        Self::safe_with_tier(TierTriggered::Tier1Heuristic, latency_us, reason)
    }

    pub fn safe_with_tier(tier: TierTriggered, latency_us: u32, reason: impl Into<String>) -> Self {
        Self {
            threat_level: ThreatLevel::Safe,
            tier_triggered: tier,
            confidence: 0.99,
            latency_us,
            category: "SAFE".to_string(),
            xai_reason: reason.into(),
            should_block: false,
        }
    }

    pub fn malicious(
        tier: TierTriggered,
        confidence: f32,
        latency_us: u32,
        category: impl Into<String>,
        reason: impl Into<String>,
    ) -> Self {
        Self {
            threat_level: ThreatLevel::Malicious,
            tier_triggered: tier,
            confidence,
            latency_us,
            category: category.into(),
            xai_reason: reason.into(),
            should_block: true,
        }
    }

    pub fn suspicious(
        tier: TierTriggered,
        confidence: f32,
        latency_us: u32,
        category: impl Into<String>,
        reason: impl Into<String>,
    ) -> Self {
        Self {
            threat_level: ThreatLevel::Suspicious,
            tier_triggered: tier,
            confidence,
            latency_us,
            category: category.into(),
            xai_reason: reason.into(),
            should_block: false,
        }
    }
}

#[repr(C)]
pub struct CScanResult {
    pub threat_level: ThreatLevel,
    pub tier_triggered: TierTriggered,
    pub confidence: f32,
    pub latency_us: u32,
    pub category: [c_char; 32],
    pub xai_reason: [c_char; 256],
    pub should_block: bool,
}

impl From<ScanResult> for CScanResult {
    fn from(r: ScanResult) -> Self {
        let mut cat_bytes = [0 as c_char; 32];
        let bytes = r.category.as_bytes();
        let len = bytes.len().min(31);
        for i in 0..len {
            cat_bytes[i] = bytes[i] as c_char;
        }

        let mut reason_bytes = [0 as c_char; 256];
        let r_bytes = r.xai_reason.as_bytes();
        let r_len = r_bytes.len().min(255);
        for i in 0..r_len {
            reason_bytes[i] = r_bytes[i] as c_char;
        }

        CScanResult {
            threat_level: r.threat_level,
            tier_triggered: r.tier_triggered,
            confidence: r.confidence,
            latency_us: r.latency_us,
            category: cat_bytes,
            xai_reason: reason_bytes,
            should_block: r.should_block,
        }
    }
}

// =========================================================================
// Communication Shield Data Types (Module A)
// =========================================================================

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum CommunicationVerdict {
    Safe,
    Suspicious,
    Spam,
    Phishing,
}

impl CommunicationVerdict {
    pub fn to_str(&self) -> &'static str {
        match self {
            CommunicationVerdict::Safe => "SAFE",
            CommunicationVerdict::Suspicious => "SUSPICIOUS",
            CommunicationVerdict::Spam => "SPAM",
            CommunicationVerdict::Phishing => "PHISHING",
        }
    }

    pub fn should_block(&self) -> bool {
        matches!(self, CommunicationVerdict::Spam | CommunicationVerdict::Phishing)
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum StirShakenStatus {
    Verified,
    Partial,
    Failed,
    NotAvailable,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CallSignals {
    pub phone_number: String,
    pub contact_match: bool,
    pub stir_shaken_status: StirShakenStatus,
    pub local_reputation_score: f32, // -1.0 (blocked) to 1.0 (trusted)
    pub call_frequency: u32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CallVerdict {
    pub phone_number: String,
    pub verdict: CommunicationVerdict,
    pub confidence: f32,
    pub xai_reasons: Vec<String>,
    pub tier: u8,
    pub latency_us: u32,
    pub should_block: bool,
    pub stir_attested: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SmsSignals {
    pub sender: String,
    pub body: String,
    pub local_reputation_score: f32,
    pub is_contact: bool,
    pub campaign_repetition_count: u32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SmsVerdict {
    pub sender: String,
    pub verdict: CommunicationVerdict,
    pub confidence: f32,
    pub xai_reasons: Vec<String>,
    pub tier: u8,
    pub latency_us: u32,
    pub extracted_urls: Vec<String>,
    pub should_quarantine: bool,
    pub is_campaign: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EmailSignals {
    pub sender_address: String,
    pub display_name: Option<String>,
    pub subject: String,
    pub body: String,
    pub spf_pass: bool,
    pub dkim_pass: bool,
    pub dmarc_pass: bool,
    pub local_reputation_score: f32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EmailVerdict {
    pub sender_address: String,
    pub verdict: CommunicationVerdict,
    pub confidence: f32,
    pub xai_reasons: Vec<String>,
    pub tier: u8,
    pub latency_us: u32,
    pub extracted_urls: Vec<String>,
    pub tracking_pixels_neutralized: u32,
    pub should_quarantine: bool,
    pub spoofing_detected: bool,
}

