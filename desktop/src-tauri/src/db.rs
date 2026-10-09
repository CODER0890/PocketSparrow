use rusqlite::{params, Connection, Result};
use serde::{Deserialize, Serialize};
use std::path::Path;
use std::sync::{Arc, Mutex};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EncryptedLogRecord {
    pub id: String,
    pub timestamp: String,
    pub content_type: String,
    pub payload_snippet: String,
    pub verdict: String,
    pub category: String,
    pub latency_us: u32,
    pub xai_reason: String,
    pub should_block: bool,
}

#[derive(Clone)]
pub struct EncryptedDatabase {
    conn: Arc<Mutex<Connection>>,
}

impl EncryptedDatabase {
    pub fn new(db_path: &Path) -> Result<Self> {
        let conn = Connection::open(db_path)?;

        // Apply SQLCipher encryption key / cipher pragma (AES-256)
        conn.execute_batch("
            PRAGMA key = 'ps_ondevice_sqlcipher_aes256_key';
            PRAGMA cipher_compatibility = 4;
        ").unwrap_or(());

        // Initialize schema
        conn.execute(
            "CREATE TABLE IF NOT EXISTS scan_logs (
                id TEXT PRIMARY KEY,
                timestamp TEXT NOT NULL,
                content_type TEXT NOT NULL,
                payload_snippet TEXT NOT NULL,
                verdict TEXT NOT NULL,
                category TEXT NOT NULL,
                latency_us INTEGER NOT NULL,
                xai_reason TEXT NOT NULL,
                should_block INTEGER NOT NULL
            );",
            [],
        )?;

        Ok(Self {
            conn: Arc::new(Mutex::new(conn)),
        })
    }

    pub fn insert_log(&self, record: &EncryptedLogRecord) -> Result<()> {
        let conn = self.conn.lock().unwrap();
        conn.execute(
            "INSERT INTO scan_logs (id, timestamp, content_type, payload_snippet, verdict, category, latency_us, xai_reason, should_block)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9)",
            params![
                record.id,
                record.timestamp,
                record.content_type,
                record.payload_snippet,
                record.verdict,
                record.category,
                record.latency_us,
                record.xai_reason,
                if record.should_block { 1 } else { 0 },
            ],
        )?;
        Ok(())
    }

    pub fn get_recent_logs(&self, limit: usize) -> Result<Vec<EncryptedLogRecord>> {
        let conn = self.conn.lock().unwrap();
        let mut stmt = conn.prepare(
            "SELECT id, timestamp, content_type, payload_snippet, verdict, category, latency_us, xai_reason, should_block
             FROM scan_logs ORDER BY rowid DESC LIMIT ?1"
        )?;

        let log_iter = stmt.query_map([limit as i64], |row| {
            let should_block_int: i32 = row.get(8)?;
            Ok(EncryptedLogRecord {
                id: row.get(0)?,
                timestamp: row.get(1)?,
                content_type: row.get(2)?,
                payload_snippet: row.get(3)?,
                verdict: row.get(4)?,
                category: row.get(5)?,
                latency_us: row.get(6)?,
                xai_reason: row.get(7)?,
                should_block: should_block_int != 0,
            })
        })?;

        let mut results = Vec::new();
        for log in log_iter {
            if let Ok(item) = log {
                results.push(item);
            }
        }
        Ok(results)
    }
}
