/**
 * Pocket Sparrow - Modern C++ Shared Detection Engine Interface
 * 100% on-device threat evaluation, zero cloud telemetry, <50ms SLA.
 */

#ifndef POCKET_SPARROW_HPP
#define POCKET_SPARROW_HPP

#include <string>
#include <vector>
#include <memory>
#include <cstdint>

namespace pocket_sparrow {

enum class Verdict : int32_t {
    Safe = 0,
    Suspicious = 1,
    Malicious = 2
};

enum class ContentType : int32_t {
    Url = 0,
    SmsText = 1,
    QrPayload = 2,
    ApkPermissions = 3
};

enum class TierTriggered : int32_t {
    Tier1Heuristic = 1,
    Tier2Transformer = 2
};

struct HeuristicResult {
    bool is_definitive{false};
    Verdict verdict{Verdict::Safe};
    float confidence{0.99f};
    std::string category{"SAFE"};
    std::string reason{""};
    uint32_t latency_us{0};
    bool needs_tier2{false};
};

struct ModelResult {
    Verdict verdict{Verdict::Safe};
    float confidence{0.0f};
    std::string category{"SAFE"};
    std::string reason{""};
    uint32_t latency_us{0};
    std::vector<std::string> salient_tokens{};
};

struct XAIExplanation {
    std::string title{};
    std::string plain_english_summary{};
    std::string technical_detail{};
    std::string recommended_action{};
};

struct QrParseResult {
    bool has_url{false};
    std::string extracted_url{};
    std::string clean_text{};
    bool is_executable_risk{false};
    std::string warning{};
};

struct PermissionAuditResult {
    uint32_t risk_score{0}; // 0 to 100
    Verdict verdict{Verdict::Safe};
    std::string category{"STANDARD_PERMISSIONS"};
    std::vector<std::string> flagged_permissions{};
    std::string explanation{};
    bool should_block{false};
};

struct ScanResult {
    Verdict verdict{Verdict::Safe};
    TierTriggered tier_triggered{TierTriggered::Tier1Heuristic};
    float confidence{0.99f};
    uint32_t latency_us{0};
    std::string category{"SAFE"};
    std::string xai_reason{""};
    bool should_block{false};
};

class DetectionEngineImpl;

class DetectionEngine {
public:
    explicit DetectionEngine(const std::string& model_path = "", const std::string& vocab_path = "");
    ~DetectionEngine();

    DetectionEngine(const DetectionEngine&) = delete;
    DetectionEngine& operator=(const DetectionEngine&) = delete;
    DetectionEngine(DetectionEngine&&) noexcept;
    DetectionEngine& operator=(DetectionEngine&&) noexcept;

    // High-level threat scanning APIs (<50ms total)
    ScanResult scan(ContentType type, const std::string& payload);
    ScanResult scan_url(const std::string& url);
    ScanResult scan_sms(const std::string& text);
    ScanResult scan_qr(const std::string& qr_payload);

    // Tier 1 and Tier 2 granular inspection
    HeuristicResult run_tier1(ContentType type, const std::string& payload);
    ModelResult run_tier2(const std::string& payload);

    // Static APK permission risk scoring
    PermissionAuditResult audit_apk_permissions(const std::vector<std::string>& permissions);

    // Explainable AI generator
    static XAIExplanation generate_xai(Verdict verdict, TierTriggered tier, const std::string& category, const std::string& reason, float confidence);

    // QR parser & URL extractor
    static QrParseResult parse_qr(const std::string& raw_qr);

private:
    std::unique_ptr<DetectionEngineImpl> impl_;
};

} // namespace pocket_sparrow

#endif // POCKET_SPARROW_HPP
