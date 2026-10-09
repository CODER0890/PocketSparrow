import 'dart:convert';
import 'package:flutter/services.dart';
import 'heuristic_engine.dart';

class ScamRegexPattern {
  final String id;
  final RegExp regex;
  final String category;
  final double severity;
  final String description;

  const ScamRegexPattern({
    required this.id,
    required this.regex,
    required this.category,
    required this.severity,
    required this.description,
  });
}

class SmsHeuristicFinding {
  final String category;
  final String description;
  final double riskWeight;
  final List<String> matchedPhrases;

  const SmsHeuristicFinding({
    required this.category,
    required this.description,
    required this.riskWeight,
    required this.matchedPhrases,
  });
}

class SmsHeuristicAnalysis {
  final String originalMessage;
  final List<SmsHeuristicFinding> findings;
  final List<String> extractedUrls;
  final List<UrlHeuristicAnalysis> urlAnalyses;
  final List<String> flaggedTokens;

  SmsHeuristicAnalysis({
    required this.originalMessage,
    required this.findings,
    required this.extractedUrls,
    required this.urlAnalyses,
    required this.flaggedTokens,
  });

  bool get hasMaliciousUrl =>
      urlAnalyses.any((analysis) => analysis.hasDefiniteBlock || analysis.compositeRiskScore >= 0.70);

  double get compositeRiskScore {
    double score = 0.0;
    for (final finding in findings) {
      score += finding.riskWeight * (1.0 - score);
    }
    for (final urlAnalysis in urlAnalyses) {
      score += urlAnalysis.compositeRiskScore * (1.0 - score);
    }
    return score.clamp(0.0, 1.0);
  }
}

class SmsHeuristicEngine {
  static final SmsHeuristicEngine _instance = SmsHeuristicEngine._internal();
  factory SmsHeuristicEngine() => _instance;
  SmsHeuristicEngine._internal();

  final HeuristicEngine _urlEngine = HeuristicEngine();

  List<String> _urgencyKeywords = [];
  List<String> _paymentLures = [];
  List<String> _fakeBankPhrases = [];
  List<ScamRegexPattern> _regexPatterns = [];
  bool _isInitialized = false;

  Future<void> initialize({String? patternsJson}) async {
    if (_isInitialized) return;
    try {
      String jsonStr;
      if (patternsJson != null) {
        jsonStr = patternsJson;
      } else {
        jsonStr = await rootBundle.loadString('assets/scam_patterns.json');
      }
      _parsePatterns(jsonStr);
      _isInitialized = true;
    } catch (_) {
      _loadDefaults();
      _isInitialized = true;
    }
  }

  void _loadDefaults() {
    _urgencyKeywords = [
      'urgent', 'urgently', 'immediately', 'immediate action', 'verify now',
      'action required', 'account suspended', 'blocked today', 'will be blocked',
      'within 24 hours', 'suspended within', 'expires today', 'deactivated',
      'act now', 'time sensitive', 'security alert', 'warning'
    ];
    _paymentLures = [
      '₹', '\$', 'refund', 'prize', 'winner', 'cashback', 'won', 'lottery',
      'bonus', 'credited to your account', 'claim reward', 'free gift',
      'kyc', 'pan card', 'aadhaar', 'update kyc', 'electricity bill',
      'bill unpaid', 'power disconnection'
    ];
    _fakeBankPhrases = [
      'your account will be blocked', 'your bank account has been locked',
      'card deactivated', 'debit card suspended', 'sbi alert', 'hdfc alert',
      'icici alert', 'axis alert', 'netbanking access suspended',
      'pan card not linked', 'complete your kyc immediately'
    ];
    _regexPatterns = [
      ScamRegexPattern(
        id: 'bank_blocked',
        regex: RegExp(r'(?:account|card|access)\s+(?:will be|has been|is)\s+(?:blocked|suspended|deactivated|frozen|locked)', caseSensitive: false),
        category: 'BANK_SPOOF',
        severity: 0.95,
        description: 'Account or payment card suspension threat',
      ),
      ScamRegexPattern(
        id: 'bank_name_spoof',
        regex: RegExp(r'\b(?:sbi|hdfc|icici|axis|kotak|pnb|canara|chase|wellsfargo|citi)\b.*(?:alert|account|notice|netbanking)', caseSensitive: false),
        category: 'BANK_SPOOF',
        severity: 0.90,
        description: 'Financial institution brand impersonation',
      ),
      ScamRegexPattern(
        id: 'electricity_power_cut',
        regex: RegExp(r'electricity\s+(?:power\s+)?(?:will be\s+)?(?:disconnected|cut|suspended)\s+(?:tonight|today|by)', caseSensitive: false),
        category: 'UTILITY_SCAM',
        severity: 0.95,
        description: 'Fraudulent electricity power disconnection threat',
      ),
      ScamRegexPattern(
        id: 'lottery_prize_won',
        regex: RegExp(r'\b(?:congratulations|won\s+(?:a\s+)?(?:prize|lottery|reward|lucky\s+draw))\b.*(?:₹|\$|rs\.?)?\s*\d+', caseSensitive: false),
        category: 'FINANCIAL_LURE',
        severity: 0.90,
        description: 'Fake lottery or contest reward claim',
      ),
    ];
  }

  void _parsePatterns(String jsonStr) {
    try {
      final data = jsonDecode(jsonStr) as Map<String, dynamic>;
      if (data['urgency_keywords'] is List) {
        _urgencyKeywords = (data['urgency_keywords'] as List).map((e) => e.toString().toLowerCase()).toList();
      }
      if (data['payment_lures'] is List) {
        _paymentLures = (data['payment_lures'] as List).map((e) => e.toString().toLowerCase()).toList();
      }
      if (data['fake_bank_phrases'] is List) {
        _fakeBankPhrases = (data['fake_bank_phrases'] as List).map((e) => e.toString().toLowerCase()).toList();
      }
      if (data['regex_patterns'] is List) {
        _regexPatterns = [];
        for (final item in data['regex_patterns']) {
          if (item is Map) {
            _regexPatterns.add(ScamRegexPattern(
              id: item['id']?.toString() ?? '',
              regex: RegExp(item['pattern']?.toString() ?? '', caseSensitive: false),
              category: item['category']?.toString() ?? 'SCAM',
              severity: (item['severity'] as num?)?.toDouble() ?? 0.85,
              description: item['description']?.toString() ?? 'Suspicious scam pattern detected',
            ));
          }
        }
      }
    } catch (_) {
      _loadDefaults();
    }
  }

  /// Extracts embedded HTTP/HTTPS and www URLs from message text
  static List<String> extractUrls(String text) {
    final urlRegex = RegExp(
      r'(https?:\/\/[^\s]+|www\.[^\s]+|[a-zA-Z0-9-]+\.[a-zA-Z]{2,}(?:\/[^\s]*)?)',
      caseSensitive: false,
    );
    final matches = urlRegex.allMatches(text);
    final List<String> results = [];
    for (final match in matches) {
      final candidate = match.group(0)!;
      final cleaned = candidate.replaceAll(RegExp(r'[\.,\?!:;]+$'), '');
      if (cleaned.contains('.')) {
        results.add(cleaned);
      }
    }
    return results;
  }

  /// Analyze full SMS message body
  SmsHeuristicAnalysis analyzeMessage(String message) {
    if (!_isInitialized) {
      _loadDefaults();
      _isInitialized = true;
    }

    final lowerMessage = message.toLowerCase();
    final List<SmsHeuristicFinding> findings = [];
    final List<String> flaggedTokens = [];

    // 1. Evaluate rich regex patterns
    for (final pattern in _regexPatterns) {
      final match = pattern.regex.firstMatch(message);
      if (match != null) {
        final phrase = match.group(0)!;
        findings.add(SmsHeuristicFinding(
          category: pattern.category,
          description: '${pattern.description} ("$phrase")',
          riskWeight: pattern.severity,
          matchedPhrases: [phrase],
        ));
        flaggedTokens.add(phrase);
      }
    }

    // 2. Fake Bank & Account Suspension Phrase checks
    final matchedBankPhrases = <String>[];
    for (final phrase in _fakeBankPhrases) {
      if (lowerMessage.contains(phrase)) {
        matchedBankPhrases.add(phrase);
        flaggedTokens.add(phrase);
      }
    }
    if (matchedBankPhrases.isNotEmpty && !findings.any((f) => f.category == 'BANK_SPOOF')) {
      findings.add(SmsHeuristicFinding(
        category: 'BANK_SPOOF',
        description: 'Impersonates institutional banking warnings ("${matchedBankPhrases.first}")',
        riskWeight: 0.85,
        matchedPhrases: matchedBankPhrases,
      ));
    }

    // 3. High Urgency Keywords
    final matchedUrgency = <String>[];
    for (final word in _urgencyKeywords) {
      if (lowerMessage.contains(word)) {
        matchedUrgency.add(word);
        flaggedTokens.add(word);
      }
    }
    if (matchedUrgency.isNotEmpty && !findings.any((f) => f.category == 'HIGH_URGENCY')) {
      findings.add(SmsHeuristicFinding(
        category: 'HIGH_URGENCY',
        description: 'Psychological pressure / urgency tactics detected (${matchedUrgency.take(3).join(', ')})',
        riskWeight: 0.60,
        matchedPhrases: matchedUrgency,
      ));
    }

    // 4. Financial, KYC, or Lottery Lures
    final matchedLures = <String>[];
    for (final lure in _paymentLures) {
      if (lowerMessage.contains(lure)) {
        matchedLures.add(lure);
        flaggedTokens.add(lure);
      }
    }
    if (matchedLures.isNotEmpty && !findings.any((f) => f.category == 'FINANCIAL_LURE' || f.category == 'KYC_FRAUD')) {
      findings.add(SmsHeuristicFinding(
        category: 'FINANCIAL_LURE',
        description: 'Financial / KYC verification bait found (${matchedLures.take(3).join(', ')})',
        riskWeight: 0.55,
        matchedPhrases: matchedLures,
      ));
    }

    // 5. URL Extraction and Chained Heuristics
    final extractedUrls = extractUrls(message);
    final List<UrlHeuristicAnalysis> urlAnalyses = [];
    for (final url in extractedUrls) {
      final analysis = _urlEngine.analyzeUrl(url);
      urlAnalyses.add(analysis);
      flaggedTokens.addAll(analysis.flaggedTokens);
    }

    // 6. Short Link + Phone/Urgency Combo Flag
    final hasShortener = urlAnalyses.any((a) =>
        a.findings.any((f) => f.code == 'URL_SHORTENER'));
    final hasUrgencyOrBank = matchedBankPhrases.isNotEmpty || matchedUrgency.isNotEmpty || findings.any((f) => f.category == 'BANK_SPOOF' || f.category == 'HIGH_URGENCY');
    if (hasShortener && hasUrgencyOrBank) {
      findings.add(const SmsHeuristicFinding(
        category: 'SHORTENER_URGENCY_COMBO',
        description: 'Dangerous combination: Obfuscated short link paired with urgent action request',
        riskWeight: 0.90,
        matchedPhrases: ['shortener + urgency'],
      ));
    }

    return SmsHeuristicAnalysis(
      originalMessage: message,
      findings: findings,
      extractedUrls: extractedUrls,
      urlAnalyses: urlAnalyses,
      flaggedTokens: flaggedTokens.toSet().toList(),
    );
  }
}
