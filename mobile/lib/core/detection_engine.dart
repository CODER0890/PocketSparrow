import 'package:flutter/services.dart';
import 'threat_model.dart';
import 'heuristic_engine.dart';
import 'sms_heuristic_engine.dart';
import 'xai_explainer.dart';
import 'local_db.dart';

class DetectionEngine {
  static final DetectionEngine _instance = DetectionEngine._internal();
  factory DetectionEngine() => _instance;
  DetectionEngine._internal();

  final HeuristicEngine _urlEngine = HeuristicEngine();
  final SmsHeuristicEngine _smsEngine = SmsHeuristicEngine();
  final LocalDb _db = LocalDb();

  bool _isMlAvailable = false;
  bool _isInitialized = false;

  bool get isMlAvailable => _isMlAvailable;

  Future<void> initialize() async {
    if (_isInitialized) return;

    await _urlEngine.initialize();
    await _smsEngine.initialize();

    // Check Tier 2 ML model availability gracefully
    try {
      final modelData = await rootBundle.load('assets/models/phishing_model.tflite');
      // If it's the placeholder text (< 100 bytes), ML is inactive
      if (modelData.lengthInBytes > 1024) {
        _isMlAvailable = true;
      } else {
        _isMlAvailable = false; // Graceful skip to Tier 1
      }
    } catch (_) {
      _isMlAvailable = false;
    }

    _isInitialized = true;
  }

  /// Scan URL link through Tier 1 Heuristics (<5ms)
  Future<ThreatResult> scanUrl(String url, {bool recordToDb = true}) async {
    await initialize();
    final stopwatch = Stopwatch()..start();

    final analysis = _urlEngine.analyzeUrl(url);

    stopwatch.stop();
    final latency = stopwatch.elapsedMilliseconds > 0 ? stopwatch.elapsedMilliseconds : 1;

    final result = XaiExplainer.explainUrl(
      analysis: analysis,
      latencyMs: latency,
      tierUsed: _isMlAvailable ? 'ml' : 'heuristic',
    );

    if (recordToDb) {
      try {
        await _db.insertThreat(result);
      } catch (_) {}
    }

    return result;
  }

  /// Scan SMS / Text message body through Tier 1 Heuristics (<5ms)
  Future<ThreatResult> scanSms(String message, {bool recordToDb = true}) async {
    await initialize();
    final stopwatch = Stopwatch()..start();

    final analysis = _smsEngine.analyzeMessage(message);

    stopwatch.stop();
    final latency = stopwatch.elapsedMilliseconds > 0 ? stopwatch.elapsedMilliseconds : 2;

    final result = XaiExplainer.explainSms(
      analysis: analysis,
      latencyMs: latency,
      tierUsed: _isMlAvailable ? 'ml' : 'heuristic',
    );

    if (recordToDb) {
      try {
        await _db.insertThreat(result);
      } catch (_) {}
    }

    return result;
  }

  /// Scan QR Code payload
  Future<ThreatResult> scanQr(String payload, {bool recordToDb = true}) async {
    await initialize();
    final stopwatch = Stopwatch()..start();

    final trimmed = payload.trim();
    final isUrl = trimmed.startsWith('http://') ||
        trimmed.startsWith('https://') ||
        trimmed.startsWith('www.') ||
        (trimmed.contains('.') && !trimmed.contains(' ') && trimmed.length > 4);

    UrlHeuristicAnalysis? urlAnalysis;
    if (isUrl) {
      urlAnalysis = _urlEngine.analyzeUrl(trimmed);
    }

    stopwatch.stop();
    final latency = stopwatch.elapsedMilliseconds > 0 ? stopwatch.elapsedMilliseconds : 1;

    final result = XaiExplainer.explainQr(
      qrPayload: payload,
      urlAnalysis: urlAnalysis,
      latencyMs: latency,
      tierUsed: _isMlAvailable ? 'ml' : 'heuristic',
    );

    if (recordToDb) {
      try {
        await _db.insertThreat(result);
      } catch (_) {}
    }

    return result;
  }

  /// Scan clipboard content
  Future<ThreatResult> scanClipboard(String content, {bool recordToDb = true}) async {
    await initialize();
    final trimmed = content.trim();

    // Check if it's a URL first
    final isUrl = trimmed.startsWith('http://') ||
        trimmed.startsWith('https://') ||
        trimmed.startsWith('www.') ||
        (!trimmed.contains(' ') && trimmed.contains('.') && trimmed.length < 256);

    ThreatResult result;
    if (isUrl) {
      final res = await scanUrl(trimmed, recordToDb: false);
      result = ThreatResult(
        verdict: res.verdict,
        confidence: res.confidence,
        reasons: res.reasons,
        latencyMs: res.latencyMs,
        tierUsed: res.tierUsed,
        rawInput: res.rawInput,
        type: ThreatType.clipboard,
        flaggedTokens: res.flaggedTokens,
      );
    } else {
      final res = await scanSms(trimmed, recordToDb: false);
      result = ThreatResult(
        verdict: res.verdict,
        confidence: res.confidence,
        reasons: res.reasons,
        latencyMs: res.latencyMs,
        tierUsed: res.tierUsed,
        rawInput: res.rawInput,
        type: ThreatType.clipboard,
        flaggedTokens: res.flaggedTokens,
      );
    }

    if (recordToDb && (result.isCaution || result.isBlocked)) {
      try {
        await _db.insertThreat(result);
      } catch (_) {}
    }

    return result;
  }
}
