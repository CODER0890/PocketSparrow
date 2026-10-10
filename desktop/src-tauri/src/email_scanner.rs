use pocket_sparrow::communication_shield::email_screener::EmailScreener;
use pocket_sparrow::engine::DetectionEngine;
use pocket_sparrow::types::{CommunicationVerdict, EmailSignals};
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
    pub tracking_pixels_neutralized: u32,
    pub spoofing_detected: bool,
}

pub struct EmailScannerService {
    #[allow(dead_code)]
    engine: Arc<DetectionEngine>,
    screener: Arc<EmailScreener>,
    accounts: Mutex<Vec<EmailAccount>>,
    history: Mutex<Vec<ScannedEmail>>,
}

impl EmailScannerService {
    pub fn new(engine: Arc<DetectionEngine>) -> Self {
        let screener = Arc::new(EmailScreener::new(engine.clone()));
        Self {
            engine,
            screener,
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
        // Parse display name if formatted as "Display Name <email@addr>" or similar
        let (display_name, clean_sender) = if let Some(start_bracket) = sender.find('<') {
            if let Some(end_bracket) = sender.find('>') {
                let disp = sender[..start_bracket].trim().trim_matches('"');
                let addr = sender[start_bracket + 1..end_bracket].trim();
                (if disp.is_empty() { None } else { Some(disp.to_string()) }, addr.to_string())
            } else {
                (None, sender.to_string())
            }
        } else {
            (None, sender.to_string())
        };

        let signals = EmailSignals {
            sender_address: clean_sender.clone(),
            display_name,
            subject: subject.to_string(),
            body: body.to_string(),
            spf_pass: !body.contains("SPF: FAIL") && !clean_sender.contains(".test") && !clean_sender.contains(".ru"),
            dkim_pass: !body.contains("DKIM: FAIL") && !clean_sender.contains(".test"),
            dmarc_pass: !body.contains("DMARC: FAIL") && !clean_sender.contains(".xyz") && !clean_sender.contains(".cfd"),
            local_reputation_score: 0.0,
        };

        let res = self.screener.screen_email(&signals);

        let verdict_str = match res.verdict {
            CommunicationVerdict::Phishing => "Malicious",
            CommunicationVerdict::Spam => "Suspicious",
            CommunicationVerdict::Suspicious => "Suspicious",
            CommunicationVerdict::Safe => "Safe",
        };

        let threat_category = match res.verdict {
            CommunicationVerdict::Phishing => {
                if res.spoofing_detected {
                    Some("BRAND_IMPERSONATION".to_string())
                } else if !res.extracted_urls.is_empty() {
                    Some("CREDENTIAL_HARVESTING".to_string())
                } else {
                    Some("PHISHING_LURE".to_string())
                }
            }
            CommunicationVerdict::Spam => Some("UNSOLICITED_SPAM".to_string()),
            CommunicationVerdict::Suspicious => Some("SUSPICIOUS_CONTENT".to_string()),
            CommunicationVerdict::Safe => Some("BENIGN".to_string()),
        };

        let xai_reason = Some(res.xai_reasons.join(" "));

        let email = ScannedEmail {
            id: format!("eml_{}", Self::current_time_ms()),
            account_id: account_id.to_string(),
            sender: sender.to_string(),
            recipient: "Protected Inbox".to_string(),
            subject: subject.to_string(),
            snippet: body.chars().take(120).collect(),
            extracted_urls: res.extracted_urls,
            verdict: verdict_str.to_string(),
            threat_category,
            xai_reason,
            latency_us: res.latency_us,
            timestamp: "Just now".to_string(),
            is_read: false,
            tracking_pixels_neutralized: res.tracking_pixels_neutralized,
            spoofing_detected: res.spoofing_detected,
        };

        let mut history = self.history.lock().unwrap();
        history.push(email.clone());
        email
    }

    pub fn clear_history(&self) {
        let mut history = self.history.lock().unwrap();
        history.clear();
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

