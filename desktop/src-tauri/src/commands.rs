use crate::db::{EncryptedDatabase, EncryptedLogRecord};
use crate::process_monitor::{DesktopProcessInfo, ProcessMonitor};
use pocket_sparrow_core::engine::DetectionEngine;
use pocket_sparrow_core::types::{ContentType, ScanResult};
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use tauri::State;

#[derive(Serialize, Deserialize)]
pub struct UiScanResult {
    pub verdict: String,
    pub tier_triggered: String,
    pub confidence: f32,
    pub latency_us: u32,
    pub category: String,
    pub xai_reason: String,
    pub should_block: bool,
}

pub struct AppState {
    pub engine: Arc<DetectionEngine>,
    pub db: Arc<EncryptedDatabase>,
    pub process_monitor: ProcessMonitor,
}

#[tauri::command]
pub async fn scan_payload(
    content_type: String,
    payload: String,
    state: State<'_, AppState>,
) -> Result<UiScanResult, String> {
    let ctype = match content_type.as_str() {
        "SmsText" => ContentType::SmsText,
        "QrPayload" => ContentType::QrPayload,
        _ => ContentType::Url,
    };

    let result = state.engine.scan(ctype, &payload);

    // Save to encrypted SQLite / SQLCipher DB
    let record = EncryptedLogRecord {
        id: format!("scan_{}", std::time::SystemTime::now().duration_since(std::time::UNIX_EPOCH).unwrap().as_millis()),
        timestamp: "Now".to_string(),
        content_type,
        payload_snippet: payload.chars().take(60).collect(),
        verdict: result.threat_level.to_str().to_string(),
        category: result.category.clone(),
        latency_us: result.latency_us,
        xai_reason: result.xai_reason.clone(),
        should_block: result.should_block,
    };
    let _ = state.db.insert_log(&record);

    Ok(UiScanResult {
        verdict: result.threat_level.to_str().to_string(),
        tier_triggered: match result.tier_triggered {
            pocket_sparrow_core::types::TierTriggered::Tier1Heuristic => "Tier1Heuristic".to_string(),
            pocket_sparrow_core::types::TierTriggered::Tier2Transformer => "Tier2Transformer".to_string(),
        },
        confidence: result.confidence,
        latency_us: result.latency_us,
        category: result.category,
        xai_reason: result.xai_reason,
        should_block: result.should_block,
    })
}

#[tauri::command]
pub async fn get_logs(state: State<'_, AppState>) -> Result<Vec<EncryptedLogRecord>, String> {
    state.db.get_recent_logs(50).map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn get_processes(state: State<'_, AppState>) -> Result<Vec<DesktopProcessInfo>, String> {
    Ok(state.process_monitor.scan_processes())
}
