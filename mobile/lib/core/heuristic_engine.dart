import 'dart:convert';
import 'dart:math' as math;
import 'package:flutter/services.dart';

class HeuristicFinding {
  final String code;
  final String description;
  final double riskWeight; // 0.0 to 1.0
  final bool isDefiniteBlock;

  const HeuristicFinding({
    required this.code,
    required this.description,
    required this.riskWeight,
    this.isDefiniteBlock = false,
  });
}

class UrlHeuristicAnalysis {
  final String originalUrl;
  final String normalizedHost;
  final double entropy;
  final List<HeuristicFinding> findings;
  final List<String> flaggedTokens;

  UrlHeuristicAnalysis({
    required this.originalUrl,
    required this.normalizedHost,
    required this.entropy,
    required this.findings,
    required this.flaggedTokens,
  });

  bool get hasDefiniteBlock => findings.any((f) => f.isDefiniteBlock);

  double get compositeRiskScore {
    if (findings.isEmpty) return 0.0;
    if (hasDefiniteBlock) return 1.0;
    // Calculate non-linear accumulated risk
    double score = 0.0;
    for (final finding in findings) {
      score += finding.riskWeight * (1.0 - score);
    }
    return score.clamp(0.0, 1.0);
  }
}

class HeuristicEngine {
  static final HeuristicEngine _instance = HeuristicEngine._internal();
  factory HeuristicEngine() => _instance;
  HeuristicEngine._internal();

  Set<String> _highRiskDomains = {};
  Set<String> _suspiciousTlds = {};
  Set<String> _shorteners = {};
  Map<String, List<String>> _protectedBrands = {};
  bool _isInitialized = false;

  // Cyrillic homoglyph lookalike map
  static const Map<String, String> _cyrillicLookalikes = {
    '\u0430': 'a', // Cyrillic Small Letter A
    '\u0410': 'A', // Cyrillic Capital Letter A
    '\u0441': 'c', // Cyrillic Small Letter Es
    '\u0421': 'C', // Cyrillic Capital Letter Es
    '\u0435': 'e', // Cyrillic Small Letter Ie
    '\u0415': 'E', // Cyrillic Capital Letter Ie
    '\u043e': 'o', // Cyrillic Small Letter O
    '\u041e': 'O', // Cyrillic Capital Letter O
    '\u0440': 'p', // Cyrillic Small Letter Er
    '\u0420': 'P', // Cyrillic Capital Letter Er
    '\u0455': 's', // Cyrillic Small Letter Dze
    '\u0405': 'S', // Cyrillic Capital Letter Dze
    '\u0456': 'i', // Cyrillic Small Letter Byelorussian-Ukrainian I
    '\u0406': 'I', // Cyrillic Capital Letter Byelorussian-Ukrainian I
    '\u0458': 'j', // Cyrillic Small Letter Je
    '\u0408': 'J', // Cyrillic Capital Letter Je
    '\u0443': 'y', // Cyrillic Small Letter U
    '\u0423': 'Y', // Cyrillic Capital Letter U
    '\u0445': 'x', // Cyrillic Small Letter Kha
    '\u0425': 'X', // Cyrillic Capital Letter Kha
    '\u03bf': 'o', // Greek Small Letter Omicron
    '\u03bd': 'v', // Greek Small Letter Nu
    '\u03c1': 'p', // Greek Small Letter Rho
  };

  Future<void> initialize({String? blocklistJson}) async {
    if (_isInitialized) return;
    try {
      String jsonStr;
      if (blocklistJson != null) {
        jsonStr = blocklistJson;
      } else {
        jsonStr = await rootBundle.loadString('assets/blocklist.json');
      }
      _parseBlocklist(jsonStr);
      _isInitialized = true;
    } catch (_) {
      // Fallback built-in blocklist defaults
      _loadDefaults();
      _isInitialized = true;
    }
  }

  void _loadDefaults() {
    _highRiskDomains = {
      'paypal-security-update.xyz',
      'paypal-login-verify.top',
      'appleid-verify-device.icu',
      'apple-security-check.cf',
      'google-auth-login.tk',
      'netflix-billing-renew.ml',
      'sbi-kyc-update.xyz',
      'hdfc-netbanking-alert.club',
      'chase-verify-activity.work',
      'amazon-security-alert.ga',
      'microsoft-account-reactivate.gq',
      'fake-bank-login.com'
    };
    _suspiciousTlds = {
      'xyz', 'top', 'club', 'tk', 'ml', 'ga', 'cf', 'gq', 'work',
      'date', 'racing', 'bid', 'icu', 'buzz', 'rest', 'surf', 'vip', 'fit', 'live'
    };
    _shorteners = {
      'bit.ly', 'tinyurl.com', 't.co', 'is.gd', 'ow.ly', 'buff.ly',
      'goo.gl', 'cutt.ly', 'rebrand.ly', 'shorturl.at'
    };
    _protectedBrands = {
      'paypal': ['paypal.com', 'paypal.me'],
      'apple': ['apple.com', 'icloud.com'],
      'google': ['google.com', 'accounts.google.com'],
      'microsoft': ['microsoft.com', 'live.com', 'outlook.com'],
      'netflix': ['netflix.com'],
      'amazon': ['amazon.com', 'amazon.in', 'amazon.co.uk'],
      'sbi': ['onlinesbi.sbi', 'sbi.co.in'],
      'hdfc': ['hdfcbank.com'],
      'chase': ['chase.com'],
      'facebook': ['facebook.com', 'meta.com'],
      'instagram': ['instagram.com'],
    };
  }

  void _parseBlocklist(String jsonStr) {
    try {
      final data = jsonDecode(jsonStr) as Map<String, dynamic>;
      if (data['high_risk_domains'] is List) {
        _highRiskDomains = (data['high_risk_domains'] as List).map((e) => e.toString().toLowerCase()).toSet();
      }
      if (data['suspicious_tlds'] is List) {
        _suspiciousTlds = (data['suspicious_tlds'] as List).map((e) => e.toString().toLowerCase().replaceAll('.', '')).toSet();
      }
      if (data['url_shorteners'] is List) {
        _shorteners = (data['url_shorteners'] as List).map((e) => e.toString().toLowerCase()).toSet();
      }
      if (data['protected_brands'] is List) {
        _protectedBrands = {};
        for (final item in data['protected_brands']) {
          if (item is Map && item['name'] != null) {
            final name = item['name'].toString().toLowerCase();
            final legits = (item['legitimate_domains'] as List? ?? []).map((e) => e.toString().toLowerCase()).toList();
            _protectedBrands[name] = legits;
          }
        }
      }
    } catch (_) {
      _loadDefaults();
    }
  }

  /// Shannon entropy calculation: H = -sum(P(i) * log2(P(i)))
  static double calculateShannonEntropy(String text) {
    if (text.isEmpty) return 0.0;
    final Map<int, int> frequencies = {};
    for (int i = 0; i < text.length; i++) {
      final code = text.codeUnitAt(i);
      frequencies[code] = (frequencies[code] ?? 0) + 1;
    }
    double entropy = 0.0;
    final int len = text.length;
    for (final count in frequencies.values) {
      final double p = count / len;
      entropy -= p * (math.log(p) / math.ln2);
    }
    return double.parse(entropy.toStringAsFixed(2));
  }

  /// Extracts domain/hostname from raw URL or bare domain
  static String extractHost(String input) {
    var raw = input.trim().toLowerCase();
    if (!raw.startsWith('http://') && !raw.startsWith('https://')) {
      raw = 'http://$raw';
    }
    String host = '';
    try {
      final uri = Uri.parse(raw);
      if (uri.host.isNotEmpty) {
        host = uri.host;
      }
    } catch (_) {}

    if (host.isEmpty) {
      // Fallback regex extraction
      final match = RegExp(r'^(?:https?:\/\/)?([^\/\?\#\:]+)').firstMatch(input.trim().toLowerCase());
      host = match?.group(1) ?? input.trim().toLowerCase();
    }

    // Decode percent-encoded Unicode (e.g. %D0%B0 -> Cyrillic 'а')
    try {
      host = Uri.decodeComponent(host);
    } catch (_) {}

    return host;
  }

  /// Analyze a given URL through all Tier 1 heuristics (<5ms)
  UrlHeuristicAnalysis analyzeUrl(String input) {
    if (!_isInitialized) {
      _loadDefaults();
      _isInitialized = true;
    }

    final trimmed = input.trim();
    final host = extractHost(trimmed);
    final List<HeuristicFinding> findings = [];
    final List<String> flaggedTokens = [];

    // 1. Direct Blocklist Check
    if (_highRiskDomains.contains(host)) {
      findings.add(HeuristicFinding(
        code: 'BLOCKLIST_MATCH',
        description: 'Domain "$host" is listed in local high-risk phishing database',
        riskWeight: 1.0,
        isDefiniteBlock: true,
      ));
      flaggedTokens.add(host);
    }

    // 2. IP Address Host Check
    final isIpv4 = RegExp(r'^(\d{1,3}\.){3}\d{1,3}(:\d+)?$').hasMatch(host);
    final isIpv6 = RegExp(r'^\[?[a-fA-F0-9:]+\]?(:\d+)?$').hasMatch(host);
    if (isIpv4 || isIpv6) {
      findings.add(const HeuristicFinding(
        code: 'RAW_IP_HOST',
        description: 'URL uses raw numeric IP address instead of registered domain name',
        riskWeight: 0.85,
        isDefiniteBlock: true,
      ));
      flaggedTokens.add(host);
    }

    // 3. Lookalike Homograph / Cyrillic Character Detection
    final homoglyphMatches = <String>[];
    for (int i = 0; i < host.length; i++) {
      final char = host[i];
      if (_cyrillicLookalikes.containsKey(char)) {
        final latinEq = _cyrillicLookalikes[char]!;
        homoglyphMatches.add('Cyrillic "$char" masquerading as Latin "$latinEq"');
      }
    }
    if (homoglyphMatches.isNotEmpty) {
      final sample = homoglyphMatches.take(3).join(', ');
      findings.add(HeuristicFinding(
        code: 'HOMOGLYPH_ATTACK',
        description: 'Punycode / Homograph attack detected: $sample',
        riskWeight: 0.95,
        isDefiniteBlock: true,
      ));
      flaggedTokens.add(host);
    }

    // 4. Punycode check (xn--)
    if (host.startsWith('xn--') || host.contains('.xn--')) {
      findings.add(const HeuristicFinding(
        code: 'PUNYCODE_DOMAIN',
        description: 'Domain uses internationalized Punycode (xn--) often used to conceal lookalikes',
        riskWeight: 0.70,
      ));
      flaggedTokens.add('xn--');
    }

    // 5. Suspicious TLD Check
    final hostParts = host.split('.');
    String tld = '';
    if (hostParts.length >= 2) {
      tld = hostParts.last;
      if (_suspiciousTlds.contains(tld)) {
        findings.add(HeuristicFinding(
          code: 'SUSPICIOUS_TLD',
          description: 'High-risk top-level domain (.$tld) frequently abused in malicious campaigns',
          riskWeight: 0.65,
        ));
        flaggedTokens.add('.$tld');
      }
    }

    // 6. Shannon Entropy of Domain
    // Calculate entropy on domain name (excluding TLD)
    final domainNameWithoutTld = hostParts.length >= 2
        ? hostParts.sublist(0, hostParts.length - 1).join('.')
        : host;
    final entropy = calculateShannonEntropy(domainNameWithoutTld);
    if (entropy > 4.0 && domainNameWithoutTld.length > 7) {
      findings.add(HeuristicFinding(
        code: 'HIGH_ENTROPY_DOMAIN',
        description: 'Shannon entropy is $entropy (> 4.0 threshold) indicating an algorithmically generated random domain (DGA)',
        riskWeight: 0.75,
      ));
      flaggedTokens.add(domainNameWithoutTld);
    }

    // 7. Excessive Subdomains (>= 3 dots or labels before TLD)
    if (hostParts.length >= 4) {
      findings.add(HeuristicFinding(
        code: 'EXCESSIVE_SUBDOMAINS',
        description: 'Contains ${hostParts.length - 2} subdomain levels, common in phishing masquerade links',
        riskWeight: 0.55,
      ));
      flaggedTokens.add(hostParts.take(hostParts.length - 2).join('.'));
    }

    // 8. Known Brand Mismatch (Brand keyword in domain or path with unauthentic apex)
    _protectedBrands.forEach((brand, legitDomains) {
      final lowerInput = trimmed.toLowerCase();
      final hasBrandInHost = host.contains(brand);
      final hasBrandInPath = lowerInput.contains('/$brand') || lowerInput.contains('brand=$brand');

      if (hasBrandInHost || hasBrandInPath) {
        final isLegit = legitDomains.any((legit) =>
            host == legit || host.endsWith('.$legit'));
        if (!isLegit) {
          findings.add(HeuristicFinding(
            code: 'BRAND_IMPERSONATION',
            description: 'Brand "${brand.toUpperCase()}" spoofing detected: host "$host" is not an official domain (${legitDomains.join(', ')})',
            riskWeight: 0.90,
            isDefiniteBlock: true,
          ));
          flaggedTokens.add(brand);
        }
      }
    });

    // 9. URL Shortener Cloaking
    if (_shorteners.contains(host)) {
      findings.add(HeuristicFinding(
        code: 'URL_SHORTENER',
        description: 'URL shortener ($host) detected; conceals true landing destination and redirect path',
        riskWeight: 0.45,
      ));
      flaggedTokens.add(host);
    }

    // 10. Credentials / @ in URL Check (e.g. http://legit.com@phish.com)
    if (trimmed.contains('@')) {
      findings.add(const HeuristicFinding(
        code: 'USERINFO_SPOOF',
        description: 'URL contains "@" sign used to trick users about the actual destination server',
        riskWeight: 0.85,
        isDefiniteBlock: true,
      ));
      flaggedTokens.add('@');
    }

    return UrlHeuristicAnalysis(
      originalUrl: trimmed,
      normalizedHost: host,
      entropy: entropy,
      findings: findings,
      flaggedTokens: flaggedTokens,
    );
  }
}
