use pocket_sparrow::engine::DetectionEngine;
use pocket_sparrow::types::{ContentType, ThreatLevel};
use serde::{Deserialize, Serialize};
use std::sync::{Arc, Mutex};
use std::time::{SystemTime, UNIX_EPOCH};

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct EmailAccount {
    pub id: String,
    pub provider: String, // "gmail" | "outlook" | "yahoo" | "imap"
    pub email: String,
    pub imap_server: String,
    pub imap_port: u16,
    pub is_active: bool,
    pub scan_incoming: bool,
    pub real_time_idle: bool,
    pub block_link_clicks: bool,
    pub notify_on_scan: bool,
    pub last_synced: String,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct ScannedEmail {
    pub id: String,
    pub account_id: String,
    pub sender: String,
    pub recipient: String,
    pub subject: String,
    pub snippet: String,
    pub extracted_urls: Vec<String>,
    pub verdict: String, // "Safe" | "Suspicious" | "Malicious"
    pub threat_category: Option<String>,
    pub xai_reason: Option<String>,
    pub latency_us: u32,
    pub timestamp: String,
    pub is_read: bool,
}

pub struct EmailScannerService {
    engine: Arc<DetectionEngine>,
    accounts: Mutex<Vec<EmailAccount>>,
    history: Mutex<Vec<ScannedEmail>>,
}

impl EmailScannerService {
    pub fn new(engine: Arc<DetectionEngine>) -> Self {
        Self {
            engine,
            accounts: Mutex::new(Vec::new()),
            history: Mutex::new(Vec::new()),
        }
    }

    pub fn get_accounts(&self) -> Vec<EmailAccount> {
        self.accounts.lock().unwrap().clone()
    }

    pub fn add_account(&self, mut account: EmailAccount) -> Result<EmailAccount, String> {
        let mut accounts = self.accounts.lock().unwrap();
        account.id = format!("acct_{}", Self::current_time_ms());
        account.last_synced = "Active (Connected)".to_string();
        accounts.push(account.clone());
        Ok(account)
    }

    pub fn remove_account(&self, id: &str) -> bool {
        let mut accounts = self.accounts.lock().unwrap();
        let initial_len = accounts.len();
        accounts.retain(|a| a.id != id);
        accounts.len() < initial_len
    }

    pub fn get_scanned_emails(&self, limit: usize) -> Vec<ScannedEmail> {
        let history = self.history.lock().unwrap();
        history.iter().rev().take(limit).cloned().collect()
    }

    pub fn scan_email_message(
        &self,
        account_id: &str,
        sender: &str,
        subject: &str,
        body: &str,
    ) -> ScannedEmail {
        let start_time = SystemTime::now();

        // 1. Extract embedded URLs from body
        let mut urls = Vec::new();
        for word in body.split_whitespace() {
            if word.starts_with("http://") || word.starts_with("https://") {
                let clean_url = word.trim_matches(|c| c == '<' || c == '>' || c == '"' || c == ',' || c == '.');
                urls.push(clean_url.to_string());
            }
        }

        // 2. Scan URLs with URL detection engine
        let mut worst_verdict = "Safe";
        let mut worst_category = None;
        let mut worst_reason = None;

        for u in &urls {
            let res = self.engine.scan(ContentType::Url, u);
            if res.threat_level == ThreatLevel::Malicious {
                worst_verdict = "Malicious";
                worst_category = Some(res.category);
                worst_reason = Some(res.xai_reason);
                break;
            } else if res.threat_level == ThreatLevel::Suspicious && worst_verdict != "Malicious" {
                worst_verdict = "Suspicious";
                worst_category = Some(res.category);
                worst_reason = Some(res.xai_reason);
            }
        }

        // 3. Scan body text with NLP/SMS smishing model
        if worst_verdict == "Safe" {
            let body_scan = self.engine.scan(ContentType::SmsText, body);
            if body_scan.threat_level == ThreatLevel::Malicious {
                worst_verdict = "Malicious";
                worst_category = Some(body_scan.category);
                worst_reason = Some(body_scan.xai_reason);
            } else if body_scan.threat_level == ThreatLevel::Suspicious {
                worst_verdict = "Suspicious";
                worst_category = Some(body_scan.category);
                worst_reason = Some(body_scan.xai_reason);
            }
        }

        let elapsed_us = start_time.elapsed().map(|d| d.as_micros() as u32).unwrap_or(12000);

        let email = ScannedEmail {
            id: format!("eml_{}", Self::current_time_ms()),
            account_id: account_id.to_string(),
            sender: sender.to_string(),
            recipient: "Protected Inbox".to_string(),
            subject: subject.to_string(),
            snippet: body.chars().take(120).collect(),
            extracted_urls: urls,
            verdict: worst_verdict.to_string(),
            threat_category: worst_category,
            xai_reason: worst_reason,
            latency_us: elapsed_us,
            timestamp: "Just now".to_string(),
            is_read: false,
        };

        let mut history = self.history.lock().unwrap();
        history.push(email.clone());
        email
    }

    fn current_time_ms() -> u64 {
        SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap_or_default()
            .as_millis() as u64
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_email_scanner_account_lifecycle() {
        let engine = Arc::new(DetectionEngine::new(None, None));
        let service = EmailScannerService::new(engine);

        assert_eq!(service.get_accounts().len(), 0);

        let acct = EmailAccount {
            id: String::new(),
            provider: "imap".to_string(),
            email: "analyst@test.local".to_string(),
            imap_server: "mail.test.local".to_string(),
            imap_port: 993,
            is_active: true,
            scan_incoming: true,
            real_time_idle: true,
            block_link_clicks: true,
            notify_on_scan: false,
            last_synced: String::new(),
        };

        let created = service.add_account(acct).unwrap();
        assert!(!created.id.is_empty());
        assert_eq!(service.get_accounts().len(), 1);

        let removed = service.remove_account(&created.id);
        assert!(removed);
        assert_eq!(service.get_accounts().len(), 0);
    }

    #[test]
    fn test_email_scanner_threat_detection() {
        let engine = Arc::new(DetectionEngine::new(None, None));
        let service = EmailScannerService::new(engine);

        let email = service.scan_email_message(
            "test_acc",
            "security@paypal.com.attacker-bank.xyz",
            "URGENT: Unauthorized Wire Transfer Detected",
            "BANK ALERT: Unusual wire transfer of $2,450.00 initiated to unknown recipient. Cancel transaction now: https://paypal.com.security-alert-center.xyz/login",
        );

        assert_eq!(email.sender, "security@paypal.com.attacker-bank.xyz");
        assert!(email.extracted_urls.iter().any(|u| u.contains("security-alert-center.xyz")));
        assert_eq!(email.verdict, "Malicious");
        assert!(email.xai_reason.is_some());
        assert_eq!(service.get_scanned_emails(10).len(), 1);
    }
}

