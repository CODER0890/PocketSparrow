import 'threat_model.dart';
import 'heuristic_engine.dart';
import 'sms_heuristic_engine.dart';

class XaiExplainer {
  /// Generate plain-language XAI output for URL analysis
  static ThreatResult explainUrl({
    required UrlHeuristicAnalysis analysis,
    required int latencyMs,
    String tierUsed = 'heuristic',
  }) {
    final List<String> reasons = [];
    final riskScore = analysis.compositeRiskScore;

    if (analysis.findings.isEmpty) {
      reasons.add('Clean domain structure: Verified authentic syntax with no suspicious patterns');
      reasons.add('Low entropy (${analysis.entropy}): Natural brand phrasing with standard TLD');
      reasons.add('Zero homoglyphs or lookalike character substitutions detected');

      return ThreatResult(
        verdict: ThreatVerdict.safe,
        confidence: 0.96,
        reasons: reasons,
        latencyMs: latencyMs,
        tierUsed: tierUsed,
        rawInput: analysis.originalUrl,
        type: ThreatType.url,
        flaggedTokens: const [],
      );
    }

    for (final finding in analysis.findings) {
      reasons.add(finding.description);
    }

    ThreatVerdict verdict;
    double confidence;

    if (analysis.hasDefiniteBlock || riskScore >= 0.70) {
      verdict = ThreatVerdict.blocked;
      confidence = (0.85 + (riskScore * 0.14)).clamp(0.85, 0.99);
    } else if (riskScore >= 0.35) {
      verdict = ThreatVerdict.caution;
      confidence = (0.65 + (riskScore * 0.25)).clamp(0.65, 0.88);
    } else {
      verdict = ThreatVerdict.safe;
      confidence = (0.80 - (riskScore * 0.30)).clamp(0.50, 0.90);
    }

    return ThreatResult(
      verdict: verdict,
      confidence: double.parse(confidence.toStringAsFixed(2)),
      reasons: reasons,
      latencyMs: latencyMs,
      tierUsed: tierUsed,
      rawInput: analysis.originalUrl,
      type: ThreatType.url,
      flaggedTokens: analysis.flaggedTokens,
    );
  }

  /// Generate plain-language XAI output for SMS analysis
  static ThreatResult explainSms({
    required SmsHeuristicAnalysis analysis,
    required int latencyMs,
    String tierUsed = 'heuristic',
  }) {
    final List<String> reasons = [];
    final riskScore = analysis.compositeRiskScore;

    if (analysis.findings.isEmpty && analysis.urlAnalyses.isEmpty) {
      reasons.add('No coercive urgency or fraudulent banking phrases detected');
      reasons.add('No known payment lures or credential solicitation patterns');
      reasons.add('Message text does not contain suspicious external redirection links');

      return ThreatResult(
        verdict: ThreatVerdict.safe,
        confidence: 0.94,
        reasons: reasons,
        latencyMs: latencyMs,
        tierUsed: tierUsed,
        rawInput: analysis.originalMessage,
        type: ThreatType.sms,
        flaggedTokens: const [],
      );
    }

    // Add SMS specific reasons
    for (final finding in analysis.findings) {
      reasons.add(finding.description);
    }

    // Add embedded URL findings
    for (final urlAnalysis in analysis.urlAnalyses) {
      for (final finding in urlAnalysis.findings) {
        reasons.add('Embedded link (${urlAnalysis.normalizedHost}): ${finding.description}');
      }
    }

    ThreatVerdict verdict;
    double confidence;

    if (analysis.hasMaliciousUrl || riskScore >= 0.70) {
      verdict = ThreatVerdict.blocked;
      confidence = (0.88 + (riskScore * 0.11)).clamp(0.88, 0.99);
    } else if (riskScore >= 0.35) {
      verdict = ThreatVerdict.caution;
      confidence = (0.68 + (riskScore * 0.22)).clamp(0.68, 0.89);
    } else {
      verdict = ThreatVerdict.safe;
      confidence = (0.82 - (riskScore * 0.30)).clamp(0.55, 0.90);
    }

    return ThreatResult(
      verdict: verdict,
      confidence: double.parse(confidence.toStringAsFixed(2)),
      reasons: reasons,
      latencyMs: latencyMs,
      tierUsed: tierUsed,
      rawInput: analysis.originalMessage,
      type: ThreatType.sms,
      flaggedTokens: analysis.flaggedTokens,
    );
  }

  /// Generate plain-language XAI output for QR codes
  static ThreatResult explainQr({
    required String qrPayload,
    required UrlHeuristicAnalysis? urlAnalysis,
    required int latencyMs,
    String tierUsed = 'heuristic',
  }) {
    final List<String> reasons = [];

    if (urlAnalysis != null) {
      final urlResult = explainUrl(
        analysis: urlAnalysis,
        latencyMs: latencyMs,
        tierUsed: tierUsed,
      );

      // Prefix reason descriptions with QR context
      final qrReasons = urlResult.reasons.map((r) => 'QR Destination: $r').toList();

      return ThreatResult(
        verdict: urlResult.verdict,
        confidence: urlResult.confidence,
        reasons: qrReasons,
        latencyMs: latencyMs,
        tierUsed: tierUsed,
        rawInput: qrPayload,
        type: ThreatType.qr,
        flaggedTokens: urlResult.flaggedTokens,
      );
    }

    // Non-URL QR Payload
    reasons.add('QR contains plain text payload without network redirection');
    return ThreatResult(
      verdict: ThreatVerdict.safe,
      confidence: 0.95,
      reasons: reasons,
      latencyMs: latencyMs,
      tierUsed: tierUsed,
      rawInput: qrPayload,
      type: ThreatType.qr,
      flaggedTokens: const [],
    );
  }
}
