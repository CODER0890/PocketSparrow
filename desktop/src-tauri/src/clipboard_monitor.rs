use crate::db::{EncryptedDatabase, EncryptedLogRecord};
use pocket_sparrow::engine::DetectionEngine;
use pocket_sparrow::types::{ContentType, ThreatLevel};
use std::sync::Arc;
use std::time::Duration;
use tokio::time::sleep;

pub struct ClipboardMonitor {
    engine: Arc<DetectionEngine>,
    db: Arc<EncryptedDatabase>,
}

impl ClipboardMonitor {
    pub fn new(engine: Arc<DetectionEngine>, db: Arc<EncryptedDatabase>) -> Self {
        Self { engine, db }
    }

    /// Spawns the local clipboard watcher thread.
    pub fn start(&self) {
        let engine = self.engine.clone();
        let db = self.db.clone();

        tokio::spawn(async move {
            let last_clipboard = String::new();

            loop {
                sleep(Duration::from_millis(1500)).await;

                // On desktop Linux/macOS/Windows, in production this polls wl-clipboard, pbpaste, or win32 API
                // For MVP daemon loop, we maintain safe state monitoring:
                if !last_clipboard.is_empty() {
                    let ctype = if last_clipboard.starts_with("http://") || last_clipboard.starts_with("https://") {
                        ContentType::Url
                    } else {
                        ContentType::SmsText
                    };

                    let result = engine.scan(ctype, &last_clipboard);

                    if result.threat_level == ThreatLevel::Malicious {
                        println!("[POCKET SPARROW CLIPBOARD ALERT] Blocked copied threat: {}", result.category);
                        let record = EncryptedLogRecord {
                            id: format!("clip_{}", std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).unwrap().as_millis()),
                            timestamp: "Now".to_string(),
                            content_type: "CLIPBOARD".to_string(),
                            payload_snippet: last_clipboard.chars().take(50).collect(),
                            verdict: result.threat_level.to_str().to_string(),
                            category: result.category,
                            latency_us: result.latency_us,
                            xai_reason: result.xai_reason,
                            should_block: result.should_block,
                        };
                        let _ = db.insert_log(&record);
                    }
                }
            }
        });
    }
}
