/**
 * Pocket Sparrow - Cross-Platform On-Device Threat Detection Engine
 * Canonical C-ABI Interface
 *
 * 100% on-device evaluation, zero network telemetry, sub-50ms latency ceiling.
 */

#ifndef POCKET_SPARROW_H
#define POCKET_SPARROW_H

#include <stdint.h>
#include <stdbool.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
    PS_CONTENT_URL = 0,
    PS_CONTENT_SMS_TEXT = 1,
    PS_CONTENT_QR_PAYLOAD = 2,
    PS_CONTENT_APK_PERMISSIONS = 3
} ps_content_type_t;

typedef enum {
    PS_THREAT_SAFE = 0,
    PS_THREAT_SUSPICIOUS = 1,
    PS_THREAT_MALICIOUS = 2
} ps_threat_level_t;

typedef enum {
    PS_TIER_1_HEURISTIC = 1,
    PS_TIER_2_TRANSFORMER = 2
} ps_tier_triggered_t;

typedef struct {
    ps_threat_level_t threat_level;
    ps_tier_triggered_t tier_triggered;
    float confidence;             /* Confidence score from 0.0 to 1.0 */
    uint32_t latency_us;          /* Execution latency in microseconds */
    char category[32];            /* Threat category string, e.g. "HOMOGRAPH", "PHISHING" */
    char xai_reason[256];         /* Human-readable explainable AI rationale */
    bool should_block;            /* Boolean block recommendation */
} ps_scan_result_t;

/**
 * Initializes the detection engine instance.
 * @param model_path Path to the INT8 model file (or NULL for heuristic-only mode).
 * @param vocab_path Path to the WordPiece vocab.txt (or NULL for default embedded vocab).
 * @return Opaque pointer to the initialized engine, or NULL on error.
 */
void* ps_engine_init(const char* model_path, const char* vocab_path);

/**
 * Frees an engine instance.
 * @param engine_handle Pointer returned by ps_engine_init.
 */
void ps_engine_free(void* engine_handle);

/**
 * Scans a content payload (URL, SMS/chat text, or decoded QR code payload).
 * Thread-safe, non-blocking, zero network calls.
 * @param engine_handle Opaque engine pointer.
 * @param content_type Type of content being analyzed.
 * @param raw_payload Null-terminated UTF-8 string to inspect.
 * @return ps_scan_result_t populated with threat assessment and XAI rationale.
 */
ps_scan_result_t ps_scan_content(
    void* engine_handle,
    ps_content_type_t content_type,
    const char* raw_payload
);

/**
 * Audits an Android application's requested permissions for dangerous combinations.
 * @param engine_handle Opaque engine pointer.
 * @param permissions Array of null-terminated permission strings.
 * @param count Number of permissions in the array.
 * @return ps_scan_result_t populated with permission risk analysis.
 */
ps_scan_result_t ps_audit_apk_permissions(
    void* engine_handle,
    const char** permissions,
    size_t count
);

#ifdef __cplusplus
}
#endif

#endif /* POCKET_SPARROW_H */
