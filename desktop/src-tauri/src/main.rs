#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod browser_bridge;
mod clipboard_monitor;
mod commands;
mod db;
mod email_scanner;
mod network_guard;
mod process_monitor;
mod spam_db;

extern crate pocket_sparrow as pocket_sparrow_core;

use browser_bridge::BrowserBridge;
use clipboard_monitor::ClipboardMonitor;
use commands::{
    add_email_account, evaluate_email_shield, get_communication_stats, get_email_accounts,
    get_hardware_metrics, get_logs, get_network_metrics, get_processes, get_scanned_emails,
    get_spam_patterns, import_offline_blocklist, remove_email_account, report_communication_spam,
    scan_incoming_email, scan_payload, terminate_process, AppState,
};
use db::EncryptedDatabase;
use email_scanner::EmailScannerService;
use network_guard::NetworkGuard;
use pocket_sparrow::communication_shield::CommunicationShield;
use pocket_sparrow::engine::DetectionEngine;
use process_monitor::ProcessMonitor;
use spam_db::SpamDatabase;
use std::path::PathBuf;
use std::sync::Arc;

#[tokio::main]
async fn main() {
    println!("================================================================");
    println!("Pocket Sparrow Desktop Threat Defense Engine");
    println!("100% On-Device Evaluation • Zero Network Telemetry • <50ms SLA");
    println!("================================================================");

    // Initialize core detection engine with INT8 ONNX model
    let model_path = "../ml/models/pocket_sparrow_int8.onnx";
    let vocab_path = "../ml/models/vocab.txt";
    let engine = Arc::new(DetectionEngine::new(Some(model_path), Some(vocab_path)));

    // Initialize encrypted local DB (SQLCipher / SQLite)
    let db_path = PathBuf::from("pocket_sparrow_local.db");
    let db = Arc::new(EncryptedDatabase::new(&db_path).expect("Failed to initialize encrypted database"));

    // Start background browser bridge on 127.0.0.1:41789
    let bridge = BrowserBridge::new(engine.clone());
    bridge.start();

    // Start clipboard monitor
    let clip = ClipboardMonitor::new(engine.clone(), db.clone());
    clip.start();

    let process_monitor = ProcessMonitor::new();
    let network_guard = Arc::new(NetworkGuard::new());
    let email_scanner = Arc::new(EmailScannerService::new(engine.clone()));
    let spam_db = Arc::new(SpamDatabase::new());
    let comm_shield = Arc::new(CommunicationShield::new(engine.clone()));

    let state = AppState {
        engine: engine.clone(),
        db: db.clone(),
        process_monitor,
        network_guard,
        email_scanner,
        spam_db,
        comm_shield,
    };

    tauri::Builder::default()
        .manage(state)
        .invoke_handler(tauri::generate_handler![
            scan_payload,
            get_logs,
            get_processes,
            terminate_process,
            get_network_metrics,
            get_hardware_metrics,
            get_email_accounts,
            add_email_account,
            remove_email_account,
            get_scanned_emails,
            scan_incoming_email,
            get_communication_stats,
            get_spam_patterns,
            report_communication_spam,
            import_offline_blocklist,
            evaluate_email_shield
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
