import 'package:flutter_test/flutter_test.dart';
import 'package:pocket_sparrow/core/heuristic_engine.dart';
import 'package:pocket_sparrow/core/sms_heuristic_engine.dart';
import 'package:pocket_sparrow/core/threat_model.dart';
import 'package:pocket_sparrow/core/xai_explainer.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late HeuristicEngine urlEngine;
  late SmsHeuristicEngine smsEngine;

  setUp(() async {
    urlEngine = HeuristicEngine();
    await urlEngine.initialize();
    smsEngine = SmsHeuristicEngine();
    await smsEngine.initialize();
  });

  group('URL Heuristic Engine Tests', () {
    test('Calculates Shannon entropy accurately', () {
      final lowEntropy = HeuristicEngine.calculateShannonEntropy('google');
      final highEntropy = HeuristicEngine.calculateShannonEntropy('x79q2m81kzp4b5n9v');
      expect(lowEntropy, lessThan(3.0));
      expect(highEntropy, greaterThan(3.8));
    });

    test('Detects Cyrillic homoglyph lookalike attack (Cyrillic а in apple.com)', () {
      // \u0430 is Cyrillic small letter a
      const homoglyphUrl = 'https://\u0430pple.com/login';
      final analysis = urlEngine.analyzeUrl(homoglyphUrl);

      expect(analysis.hasDefiniteBlock, isTrue);
      expect(analysis.findings.any((f) => f.code == 'HOMOGLYPH_ATTACK'), isTrue);

      final explained = XaiExplainer.explainUrl(analysis: analysis, latencyMs: 2);
      expect(explained.verdict, equals(ThreatVerdict.blocked));
      expect(explained.reasons.any((r) => r.contains('Homograph')), isTrue);
    });

    test('Detects raw IP-based URLs', () {
      final analysis = urlEngine.analyzeUrl('http://192.168.1.1/admin');
      expect(analysis.hasDefiniteBlock, isTrue);
      expect(analysis.findings.any((f) => f.code == 'RAW_IP_HOST'), isTrue);
    });

    test('Detects suspicious TLDs (.xyz, .top)', () {
      final analysis = urlEngine.analyzeUrl('https://my-secure-portal.xyz/login');
      expect(analysis.findings.any((f) => f.code == 'SUSPICIOUS_TLD'), isTrue);
    });

    test('Detects Brand Impersonation (paypal spoofing)', () {
      final analysis = urlEngine.analyzeUrl('https://paypal-security-update.xyz/verify');
      expect(analysis.findings.any((f) => f.code == 'BRAND_IMPERSONATION'), isTrue);
      expect(analysis.hasDefiniteBlock, isTrue);

      final explained = XaiExplainer.explainUrl(analysis: analysis, latencyMs: 3);
      expect(explained.verdict, equals(ThreatVerdict.blocked));
    });

    test('Identifies safe legitimate domain cleanly', () {
      final analysis = urlEngine.analyzeUrl('https://google.com');
      expect(analysis.findings.isEmpty, isTrue);

      final explained = XaiExplainer.explainUrl(analysis: analysis, latencyMs: 1);
      expect(explained.verdict, equals(ThreatVerdict.safe));
      expect(explained.confidence, greaterThanOrEqualTo(0.90));
    });

    test('Sub-5ms execution benchmark', () {
      final stopwatch = Stopwatch()..start();
      const testUrl = 'https://paypal-security-update.xyz/verify?session=123';
      const iterations = 50;

      for (int i = 0; i < iterations; i++) {
        urlEngine.analyzeUrl(testUrl);
      }
      stopwatch.stop();

      final avgMs = stopwatch.elapsedMilliseconds / iterations;
      // Heuristics must execute well under 5ms per check
      expect(avgMs, lessThan(5.0));
    });
  });

  group('SMS Heuristic Engine Tests', () {
    test('Flags high-urgency banking scam with embedded phishing link', () {
      const sms = 'URGENT: Your SBI account will be blocked. Verify now at http://sbi-kyc-update.xyz';
      final analysis = smsEngine.analyzeMessage(sms);

      expect(analysis.findings.any((f) => f.category == 'BANK_SPOOF'), isTrue);
      expect(analysis.findings.any((f) => f.category == 'HIGH_URGENCY'), isTrue);
      expect(analysis.hasMaliciousUrl, isTrue);

      final explained = XaiExplainer.explainSms(analysis: analysis, latencyMs: 3);
      expect(explained.verdict, equals(ThreatVerdict.blocked));
      expect(explained.reasons.length, greaterThanOrEqualTo(2));
    });

    test('Identifies benign conversational SMS as safe', () {
      const sms = 'Hey, are we still meeting for lunch tomorrow at 1pm?';
      final analysis = smsEngine.analyzeMessage(sms);

      expect(analysis.findings.isEmpty, isTrue);
      expect(analysis.urlAnalyses.isEmpty, isTrue);

      final explained = XaiExplainer.explainSms(analysis: analysis, latencyMs: 2);
      expect(explained.verdict, equals(ThreatVerdict.safe));
    });
  });
}
