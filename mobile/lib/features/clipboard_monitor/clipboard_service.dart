import 'dart:async';
import 'package:flutter/services.dart';
import '../../core/detection_engine.dart';
import '../../core/threat_model.dart';

class ClipboardItemRecord {
  final String content;
  final DateTime timestamp;
  final ThreatResult result;

  ClipboardItemRecord({
    required this.content,
    required this.timestamp,
    required this.result,
  });
}

class ClipboardService {
  static final ClipboardService _instance = ClipboardService._internal();
  factory ClipboardService() => _instance;
  ClipboardService._internal();

  final DetectionEngine _engine = DetectionEngine();
  Timer? _pollingTimer;
  String? _lastText;
  bool _isMonitoring = false;

  final List<ClipboardItemRecord> _history = [];
  final StreamController<ClipboardItemRecord> _threatStreamController =
      StreamController<ClipboardItemRecord>.broadcast();

  bool get isMonitoring => _isMonitoring;
  List<ClipboardItemRecord> get history => List.unmodifiable(_history);
  Stream<ClipboardItemRecord> get onThreatDetected => _threatStreamController.stream;

  void startMonitoring({Duration interval = const Duration(seconds: 2)}) {
    if (_isMonitoring) return;
    _isMonitoring = true;
    _pollingTimer = Timer.periodic(interval, (_) => _checkClipboard());
  }

  void stopMonitoring() {
    _pollingTimer?.cancel();
    _pollingTimer = null;
    _isMonitoring = false;
  }

  Future<ClipboardItemRecord?> checkNow() async {
    return await _checkClipboard(force: true);
  }

  Future<ClipboardItemRecord?> _checkClipboard({bool force = false}) async {
    try {
      final data = await Clipboard.getData(Clipboard.kTextPlain);
      final text = data?.text?.trim();

      if (text == null || text.isEmpty) return null;
      if (!force && text == _lastText) return null;

      _lastText = text;

      // Analyze the clipboard content
      final result = await _engine.scanClipboard(text);
      final record = ClipboardItemRecord(
        content: text,
        timestamp: DateTime.now(),
        result: result,
      );

      _history.insert(0, record);
      if (_history.length > 30) {
        _history.removeLast();
      }

      if (result.isBlocked || result.isCaution) {
        _threatStreamController.add(record);
      }

      return record;
    } catch (_) {
      return null;
    }
  }

  void clearHistory() {
    _history.clear();
  }

  void dispose() {
    stopMonitoring();
    _threatStreamController.close();
  }
}
