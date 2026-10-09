import 'dart:async';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'threat_model.dart';
import 'detection_engine.dart';
import 'local_db.dart';
import '../features/clipboard_monitor/clipboard_service.dart';

class AppStateProvider extends ChangeNotifier {
  final DetectionEngine _engine = DetectionEngine();
  final LocalDb _db = LocalDb();
  final ClipboardService _clipboardService = ClipboardService();

  bool _isProtectionActive = true;
  bool _isClipboardMonitoring = true;
  bool _isHapticsEnabled = true;
  bool _isAirplaneModeSimulated = true;
  bool _isScanning = false;

  List<ThreatResult> _recentThreats = [];
  Map<String, dynamic> _stats = {
    'total': 0,
    'blockedToday': 0,
    'avgLatencyMs': 3,
    'cloudCalls': 0,
  };

  ThreatResult? _latestResult;
  StreamSubscription<ClipboardItemRecord>? _clipboardSub;

  // Getters
  bool get isProtectionActive => _isProtectionActive;
  bool get isClipboardMonitoring => _isClipboardMonitoring;
  bool get isHapticsEnabled => _isHapticsEnabled;
  bool get isAirplaneModeSimulated => _isAirplaneModeSimulated;
  bool get isScanning => _isScanning;
  List<ThreatResult> get recentThreats => _recentThreats;
  Map<String, dynamic> get stats => _stats;
  ThreatResult? get latestResult => _latestResult;
  ClipboardService get clipboardService => _clipboardService;
  bool get isMlAvailable => _engine.isMlAvailable;

  AppStateProvider() {
    _initialize();
  }

  Future<void> _initialize() async {
    await _engine.initialize();
    final prefs = await SharedPreferences.getInstance();
    _isProtectionActive = prefs.getBool('isProtectionActive') ?? true;
    _isClipboardMonitoring = prefs.getBool('isClipboardMonitoring') ?? true;
    _isHapticsEnabled = prefs.getBool('isHapticsEnabled') ?? true;
    _isAirplaneModeSimulated = prefs.getBool('isAirplaneModeSimulated') ?? true;

    if (_isClipboardMonitoring && _isProtectionActive) {
      _clipboardService.startMonitoring();
    }

    _clipboardSub = _clipboardService.onThreatDetected.listen((item) {
      _latestResult = item.result;
      refreshData();
    });

    await refreshData();
  }

  Future<void> refreshData() async {
    try {
      _recentThreats = await _db.getRecentThreats(limit: 5);
      _stats = await _db.getThreatStats();
    } catch (_) {}
    notifyListeners();
  }

  Future<ThreatResult> scanUrl(String url) async {
    _isScanning = true;
    notifyListeners();
    try {
      final res = await _engine.scanUrl(url);
      _latestResult = res;
      await refreshData();
      return res;
    } finally {
      _isScanning = false;
      notifyListeners();
    }
  }

  Future<ThreatResult> scanSms(String text) async {
    _isScanning = true;
    notifyListeners();
    try {
      final res = await _engine.scanSms(text);
      _latestResult = res;
      await refreshData();
      return res;
    } finally {
      _isScanning = false;
      notifyListeners();
    }
  }

  Future<ThreatResult> scanQr(String payload) async {
    _isScanning = true;
    notifyListeners();
    try {
      final res = await _engine.scanQr(payload);
      _latestResult = res;
      await refreshData();
      return res;
    } finally {
      _isScanning = false;
      notifyListeners();
    }
  }

  Future<ThreatResult> scanClipboardDirect(String content) async {
    _isScanning = true;
    notifyListeners();
    try {
      final res = await _engine.scanClipboard(content);
      _latestResult = res;
      await refreshData();
      return res;
    } finally {
      _isScanning = false;
      notifyListeners();
    }
  }

  Future<void> toggleProtection(bool val) async {
    _isProtectionActive = val;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('isProtectionActive', val);
    if (!val) {
      _clipboardService.stopMonitoring();
    } else if (_isClipboardMonitoring) {
      _clipboardService.startMonitoring();
    }
    notifyListeners();
  }

  Future<void> toggleClipboardMonitoring(bool val) async {
    _isClipboardMonitoring = val;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('isClipboardMonitoring', val);
    if (val && _isProtectionActive) {
      _clipboardService.startMonitoring();
    } else {
      _clipboardService.stopMonitoring();
    }
    notifyListeners();
  }

  Future<void> toggleHaptics(bool val) async {
    _isHapticsEnabled = val;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('isHapticsEnabled', val);
    notifyListeners();
  }

  Future<void> toggleAirplaneModeSimulated(bool val) async {
    _isAirplaneModeSimulated = val;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('isAirplaneModeSimulated', val);
    notifyListeners();
  }

  Future<void> clearAllLogs() async {
    await _db.clearAll();
    _clipboardService.clearHistory();
    _latestResult = null;
    await refreshData();
  }

  Future<void> resetToProductionState() async {
    _isAirplaneModeSimulated = false;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('isAirplaneModeSimulated', false);
    await clearAllLogs();
  }

  bool _isDisposed = false;

  @override
  void notifyListeners() {
    if (!_isDisposed) {
      super.notifyListeners();
    }
  }

  @override
  void dispose() {
    _isDisposed = true;
    _clipboardSub?.cancel();
    _clipboardService.dispose();
    super.dispose();
  }
}
