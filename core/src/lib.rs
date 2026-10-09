pub mod engine;
pub mod jni;
pub mod qr;
pub mod tier1;
pub mod tier2;
pub mod types;
pub mod xai;

pub use engine::DetectionEngine;
pub use types::{
    CScanResult, ContentType, HeuristicResult, ModelResult, PermissionAuditResult, ScanResult,
    ThreatLevel, TierTriggered, Verdict, XAIExplanation,
};

use std::ffi::CStr;
use std::os::raw::c_char;

/// Initializes the detection engine instance.
#[no_mangle]
pub unsafe extern "C" fn ps_engine_init(
    model_path: *const c_char,
    vocab_path: *const c_char,
) -> *mut DetectionEngine {
    let m_path = if !model_path.is_null() {
        CStr::from_ptr(model_path).to_str().ok()
    } else {
        None
    };

    let v_path = if !vocab_path.is_null() {
        CStr::from_ptr(vocab_path).to_str().ok()
    } else {
        None
    };

    let engine = Box::new(DetectionEngine::new(m_path, v_path));
    Box::into_raw(engine)
}

/// Frees an engine instance.
#[no_mangle]
pub unsafe extern "C" fn ps_engine_free(engine_handle: *mut DetectionEngine) {
    if !engine_handle.is_null() {
        let _ = Box::from_raw(engine_handle);
    }
}

/// Scans content via C-ABI interface. Thread-safe, 0 network access, <50ms.
#[no_mangle]
pub unsafe extern "C" fn ps_scan_content(
    engine_handle: *mut DetectionEngine,
    content_type: types::ContentType,
    raw_payload: *const c_char,
) -> CScanResult {
    if engine_handle.is_null() || raw_payload.is_null() {
        return ScanResult::safe(0, "Invalid engine handle or empty payload").into();
    }

    let engine = &*engine_handle;
    let payload = match CStr::from_ptr(raw_payload).to_str() {
        Ok(s) => s,
        Err(_) => return ScanResult::safe(0, "Invalid UTF-8 payload").into(),
    };

    let result = engine.scan(content_type, payload);
    result.into()
}

/// Audits an application's requested permissions via C-ABI.
#[no_mangle]
pub unsafe extern "C" fn ps_audit_apk_permissions(
    engine_handle: *mut DetectionEngine,
    permissions: *const *const c_char,
    count: usize,
) -> CScanResult {
    if engine_handle.is_null() || permissions.is_null() || count == 0 {
        return ScanResult::safe(0, "Empty permissions list").into();
    }

    let engine = &*engine_handle;
    let mut perm_strings = Vec::with_capacity(count);

    for i in 0..count {
        let ptr = *permissions.add(i);
        if !ptr.is_null() {
            if let Ok(s) = CStr::from_ptr(ptr).to_str() {
                perm_strings.push(s);
            }
        }
    }

    let result = engine.audit_permissions(&perm_strings);
    result.into()
}
