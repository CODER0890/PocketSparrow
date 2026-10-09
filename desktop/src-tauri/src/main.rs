#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod browser_bridge;
mod clipboard_monitor;
mod commands;
mod db;
mod network_guard;
mod process_monitor;

extern crate pocket_sparrow as pocket_sparrow_core;

use browser_bridge::BrowserBridge;
use clipboard_monitor::ClipboardMonitor;
use commands::{AppState, get_logs, get_processes, scan_payload, terminate_process};
use db::EncryptedDatabase;
use pocket_sparrow::engine::DetectionEngine;
use process_monitor::ProcessMonitor;
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

    let state = AppState {
        engine: engine.clone(),
        db: db.clone(),
        process_monitor,
    };

    tauri::Builder::default()
        .manage(state)
        .invoke_handler(tauri::generate_handler![
            scan_payload,
            get_logs,
            get_processes,
            terminate_process
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
