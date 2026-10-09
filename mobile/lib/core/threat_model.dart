import 'dart:convert';

enum ThreatVerdict {
  safe,
  caution,
  blocked;

  String get displayName {
    switch (this) {
      case ThreatVerdict.safe:
        return 'Safe';
      case ThreatVerdict.caution:
        return 'Caution';
      case ThreatVerdict.blocked:
        return 'Blocked';
    }
  }

  static ThreatVerdict fromString(String val) {
    switch (val.toLowerCase()) {
      case 'blocked':
        return ThreatVerdict.blocked;
      case 'caution':
        return ThreatVerdict.caution;
      case 'safe':
      default:
        return ThreatVerdict.safe;
    }
  }
}

enum ThreatType {
  url,
  sms,
  qr,
  clipboard;

  String get displayName {
    switch (this) {
      case ThreatType.url:
        return 'URL Link';
      case ThreatType.sms:
        return 'SMS / Text';
      case ThreatType.qr:
        return 'QR Code';
      case ThreatType.clipboard:
        return 'Clipboard';
    }
  }

  static ThreatType fromString(String val) {
    switch (val.toLowerCase()) {
      case 'sms':
        return ThreatType.sms;
      case 'qr':
        return ThreatType.qr;
      case 'clipboard':
        return ThreatType.clipboard;
      case 'url':
      default:
        return ThreatType.url;
    }
  }
}

class ThreatResult {
  final int? id;
  final ThreatVerdict verdict;
  final double confidence;
  final List<String> reasons;
  final int latencyMs;
  final String tierUsed;
  final String rawInput;
  final ThreatType type;
  final DateTime timestamp;
  final List<String> flaggedTokens;

  ThreatResult({
    this.id,
    required this.verdict,
    required this.confidence,
    required this.reasons,
    required this.latencyMs,
    this.tierUsed = 'heuristic',
    required this.rawInput,
    required this.type,
    DateTime? timestamp,
    this.flaggedTokens = const [],
  }) : timestamp = timestamp ?? DateTime.now();

  bool get isSafe => verdict == ThreatVerdict.safe;
  bool get isCaution => verdict == ThreatVerdict.caution;
  bool get isBlocked => verdict == ThreatVerdict.blocked;

  Map<String, dynamic> toJson() {
    return {
      'verdict': verdict.name,
      'confidence': double.parse(confidence.toStringAsFixed(2)),
      'reasons': reasons,
      'latency_ms': latencyMs,
      'tier_used': tierUsed,
    };
  }

  Map<String, dynamic> toDbMap() {
    return {
      if (id != null) 'id': id,
      'verdict': verdict.name,
      'confidence': confidence,
      'reasons': jsonEncode(reasons),
      'latency_ms': latencyMs,
      'tier_used': tierUsed,
      'raw_input': rawInput,
      'type': type.name,
      'timestamp': timestamp.toIso8601String(),
      'flagged_tokens': jsonEncode(flaggedTokens),
    };
  }

  factory ThreatResult.fromDbMap(Map<String, dynamic> map) {
    List<String> parsedReasons = [];
    if (map['reasons'] != null) {
      try {
        final decoded = jsonDecode(map['reasons'] as String);
        if (decoded is List) {
          parsedReasons = decoded.map((e) => e.toString()).toList();
        }
      } catch (_) {
        parsedReasons = [map['reasons'].toString()];
      }
    }

    List<String> parsedTokens = [];
    if (map['flagged_tokens'] != null) {
      try {
        final decoded = jsonDecode(map['flagged_tokens'] as String);
        if (decoded is List) {
          parsedTokens = decoded.map((e) => e.toString()).toList();
        }
      } catch (_) {}
    }

    return ThreatResult(
      id: map['id'] as int?,
      verdict: ThreatVerdict.fromString(map['verdict'] as String? ?? 'safe'),
      confidence: (map['confidence'] as num?)?.toDouble() ?? 0.0,
      reasons: parsedReasons,
      latencyMs: (map['latency_ms'] as num?)?.toInt() ?? 0,
      tierUsed: map['tier_used'] as String? ?? 'heuristic',
      rawInput: map['raw_input'] as String? ?? '',
      type: ThreatType.fromString(map['type'] as String? ?? 'url'),
      timestamp: map['timestamp'] != null
          ? DateTime.tryParse(map['timestamp'] as String) ?? DateTime.now()
          : DateTime.now(),
      flaggedTokens: parsedTokens,
    );
  }
}
