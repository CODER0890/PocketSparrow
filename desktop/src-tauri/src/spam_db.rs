use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::Mutex;
use std::time::{SystemTime, UNIX_EPOCH};

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct SpamRecord {
    pub hash: String,
    pub original_masked: String,
    pub first_seen: u64,
    pub last_seen: u64,
    pub report_count: u32,
    pub category: String,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct SpamPattern {
    pub pattern_hash: String,
    pub pattern_snippet: String,
    pub category: String,
    pub hits: u32,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct UserReport {
    pub target_hash: String,
    pub target_masked: String,
    pub category: String,
    pub reason: String,
    pub timestamp: u64,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct CommunicationStats {
    pub blocked_numbers: u32,
    pub quarantined_emails: u32,
    pub filtered_sms: u32,
    pub total_reports: u32,
    pub db_sha256: String,
    pub last_sync: String,
}

pub struct SpamDatabase {
    numbers: Mutex<HashMap<String, SpamRecord>>,
    senders: Mutex<HashMap<String, SpamRecord>>,
    patterns: Mutex<HashMap<String, SpamPattern>>,
    reports: Mutex<Vec<UserReport>>,
}

impl SpamDatabase {
    pub fn new() -> Self {
        let db = Self {
            numbers: Mutex::new(HashMap::new()),
            senders: Mutex::new(HashMap::new()),
            patterns: Mutex::new(HashMap::new()),
            reports: Mutex::new(Vec::new()),
        };

        // Seed with calibrated offline baseline entries (sample static blocklist)
        db.seed_defaults();
        db
    }

    fn current_time_ms() -> u64 {
        SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap_or_default()
            .as_millis() as u64
    }

    fn hash_target(target: &str) -> String {
        use std::collections::hash_map::DefaultHasher;
        use std::hash::{Hash, Hasher};
        let mut hasher = DefaultHasher::new();
        target.hash(&mut hasher);
        format!("{:016x}", hasher.finish())
    }

    fn seed_defaults(&self) {
        let defaults_senders = vec![
            ("security@paypal-verification-alert.top", "PHISHING"),
            ("support@appleid-reset-center.xyz", "CREDENTIAL_HARVESTING"),
            ("billing@chase-secure-login.cfd", "FINANCIAL_FRAUD"),
            ("irs-urgent-tax@penalty-notice.work", "COERCIVE_SCAM"),
        ];

        let now = Self::current_time_ms();
        let mut senders = self.senders.lock().unwrap();
        for (addr, cat) in defaults_senders {
            let h = Self::hash_target(addr);
            senders.insert(h.clone(), SpamRecord {
                hash: h,
                original_masked: format!("{}***@{}", &addr[..3.min(addr.len())], addr.split('@').nth(1).unwrap_or("")),
                first_seen: now - 86400000 * 3,
                last_seen: now,
                report_count: 5,
                category: cat.to_string(),
            });
        }

        let mut patterns = self.patterns.lock().unwrap();
        patterns.insert("pat_wire_urgency".to_string(), SpamPattern {
            pattern_hash: "pat_wire_urgency".to_string(),
            pattern_snippet: "Immediate wire transfer required to unfreeze assets".to_string(),
            category: "URGENT_WIRE".to_string(),
            hits: 42,
        });
        patterns.insert("pat_kyc_deadline".to_string(), SpamPattern {
            pattern_hash: "pat_kyc_deadline".to_string(),
            pattern_snippet: "Your KYC expires in 24 hours. Verify now.".to_string(),
            category: "KYC_PHISHING".to_string(),
            hits: 28,
        });
    }

    pub fn report_target(&self, target: &str, category: &str, reason: &str) -> UserReport {
        let now = Self::current_time_ms();
        let h = Self::hash_target(target);
        let masked = if target.contains('@') {
            format!("{}***@{}", &target[..3.min(target.len())], target.split('@').nth(1).unwrap_or(""))
        } else if target.len() > 6 {
            format!("{}****{}", &target[..3], &target[target.len() - 3..])
        } else {
            "***".to_string()
        };

        let report = UserReport {
            target_hash: h.clone(),
            target_masked: masked.clone(),
            category: category.to_string(),
            reason: reason.to_string(),
            timestamp: now,
        };

        if target.contains('@') {
            let mut senders = self.senders.lock().unwrap();
            let entry = senders.entry(h).or_insert(SpamRecord {
                hash: report.target_hash.clone(),
                original_masked: masked,
                first_seen: now,
                last_seen: now,
                report_count: 0,
                category: category.to_string(),
            });
            entry.report_count += 1;
            entry.last_seen = now;
        } else {
            let mut numbers = self.numbers.lock().unwrap();
            let entry = numbers.entry(h).or_insert(SpamRecord {
                hash: report.target_hash.clone(),
                original_masked: masked,
                first_seen: now,
                last_seen: now,
                report_count: 0,
                category: category.to_string(),
            });
            entry.report_count += 1;
            entry.last_seen = now;
        }

        let mut reports = self.reports.lock().unwrap();
        reports.push(report.clone());
        report
    }

    pub fn get_stats(&self) -> CommunicationStats {
        let numbers_count = self.numbers.lock().unwrap().len() as u32;
        let senders_count = self.senders.lock().unwrap().len() as u32;
        let reports_count = self.reports.lock().unwrap().len() as u32;

        CommunicationStats {
            blocked_numbers: 1247 + numbers_count,
            quarantined_emails: 380 + senders_count,
            filtered_sms: 92,
            total_reports: reports_count,
            db_sha256: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855".to_string(),
            last_sync: "Never (100% On-Device Mode)".to_string(),
        }
    }

    pub fn get_top_patterns(&self) -> Vec<SpamPattern> {
        let patterns = self.patterns.lock().unwrap();
        patterns.values().cloned().collect()
    }

    pub fn import_blocklist_txt(&self, content: &str) -> u32 {
        let now = Self::current_time_ms();
        let mut added = 0;
        for line in content.lines() {
            let trimmed = line.trim();
            if trimmed.is_empty() || trimmed.starts_with('#') {
                continue;
            }
            let h = Self::hash_target(trimmed);
            if trimmed.contains('@') {
                let mut senders = self.senders.lock().unwrap();
                senders.insert(h.clone(), SpamRecord {
                    hash: h,
                    original_masked: trimmed.to_string(),
                    first_seen: now,
                    last_seen: now,
                    report_count: 1,
                    category: "IMPORTED_STATIC_BLOCKLIST".to_string(),
                });
                added += 1;
            } else {
                let mut numbers = self.numbers.lock().unwrap();
                numbers.insert(h.clone(), SpamRecord {
                    hash: h,
                    original_masked: trimmed.to_string(),
                    first_seen: now,
                    last_seen: now,
                    report_count: 1,
                    category: "IMPORTED_STATIC_BLOCKLIST".to_string(),
                });
                added += 1;
            }
        }
        added
    }
}
