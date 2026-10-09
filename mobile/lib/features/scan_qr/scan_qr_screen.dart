import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_lucide/flutter_lucide.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:provider/provider.dart';
import '../../core/app_state_provider.dart';
import '../../core/threat_model.dart';
import '../../ui/theme/app_colors.dart';
import '../../ui/theme/app_radius.dart';
import '../../ui/theme/app_shadows.dart';
import '../../ui/theme/app_spacing.dart';
import '../../ui/theme/app_text_styles.dart';
import '../../ui/widgets/app_button.dart';
import '../../ui/widgets/app_card.dart';
import '../../ui/widgets/app_list_tile.dart';
import '../../ui/widgets/app_scaffold.dart';
import '../../ui/widgets/input_field.dart';
import '../../ui/widgets/scanning_radar.dart';
import '../../ui/widgets/section_header.dart';
import '../../ui/widgets/xai_breakdown_card.dart';

class ScanQrScreen extends StatefulWidget {
  const ScanQrScreen({super.key});

  @override
  State<ScanQrScreen> createState() => _ScanQrScreenState();
}

class _ScanQrScreenState extends State<ScanQrScreen> with SingleTickerProviderStateMixin {
  late MobileScannerController _scannerController;
  late AnimationController _laserController;
  late Animation<double> _laserAnimation;

  final TextEditingController _qrTextController = TextEditingController();
  ThreatResult? _currentResult;
  String _decodedPayload = '';
  bool _isTorchOn = false;
  bool _isScannerPaused = false;
  DateTime? _lastScannedTime;

  final List<Map<String, String>> _manualTestVectors = [
    {
      'title': 'Cyrillic Apple QR (Homoglyph Spoof)',
      'payload': 'https://\u0430pple.com/login',
      'tag': 'BLOCKED',
    },
    {
      'title': 'PayPal Phish QR (.xyz Domain)',
      'payload': 'https://paypal-security-update.xyz/verify?qr=1',
      'tag': 'BLOCKED',
    },
    {
      'title': 'Shortener Redirect QR (URL Cloak)',
      'payload': 'https://bit.ly/fake-bank-promo',
      'tag': 'CAUTION',
    },
    {
      'title': 'Safe Official Portal QR (Google)',
      'payload': 'https://google.com',
      'tag': 'SAFE',
    },
    {
      'title': 'Plain Wi-Fi Text QR (No Redirection)',
      'payload': 'WIFI:S:Secure_HQ;T:WPA;P:cybersparrow2026;;',
      'tag': 'SAFE',
    },
    {
      'title': 'Malicious Direct APK Link QR',
      'payload': 'https://security-patch-download.ru/update.apk',
      'tag': 'BLOCKED',
    },
  ];

  @override
  void initState() {
    super.initState();
    _scannerController = MobileScannerController(
      detectionSpeed: DetectionSpeed.noDuplicates,
      facing: CameraFacing.back,
      torchEnabled: false,
      autoStart: true,
    );

    _laserController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 2200),
    )..repeat(reverse: true);

    _laserAnimation = Tween<double>(begin: 0.08, end: 0.92).animate(
      CurvedAnimation(parent: _laserController, curve: Curves.easeInOut),
    );
  }

  @override
  void dispose() {
    _laserController.dispose();
    try {
      _scannerController.dispose();
    } catch (_) {}
    _qrTextController.dispose();
    super.dispose();
  }

  Future<void> _processQr(String payload) async {
    final trimmed = payload.trim();
    if (trimmed.isEmpty) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Please enter or scan a valid QR payload')),
        );
      }
      return;
    }

    // Debounce camera triggers within 1.5 seconds
    final now = DateTime.now();
    if (_lastScannedTime != null && now.difference(_lastScannedTime!).inMilliseconds < 1500) {
      if (_decodedPayload == trimmed) {
        return;
      }
    }
    _lastScannedTime = now;

    setState(() {
      _decodedPayload = trimmed;
      _qrTextController.text = trimmed;
    });

    final state = context.read<AppStateProvider>();
    if (state.isHapticsEnabled) {
      HapticFeedback.mediumImpact();
    }

    try {
      final res = await state.scanQr(trimmed);
      if (mounted) {
        setState(() {
          _currentResult = res;
        });
      }
    } catch (e) {
      debugPrint('Error scanning QR: $e');
    }
  }

  Future<void> _pasteFromClipboard() async {
    final data = await Clipboard.getData(Clipboard.kTextPlain);
    final text = data?.text?.trim() ?? '';
    if (text.isEmpty) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Clipboard is currently empty')),
        );
      }
      return;
    }
    _qrTextController.text = text;
    await _processQr(text);
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppStateProvider>();

    return AppScaffold(
      title: Text(
        'Scan QR Code',
        style: AppTextStyles.headingLarge,
      ),
      body: SingleChildScrollView(
        padding: AppSpacing.paddingLg,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Live Camera Viewfinder Box
            Container(
              height: 250,
              width: double.infinity,
              decoration: BoxDecoration(
                color: Colors.black,
                borderRadius: AppRadius.card,
                border: Border.all(color: AppColors.primary.withOpacity(0.5), width: 1.5),
                boxShadow: AppShadows.soft,
              ),
              child: ClipRRect(
                borderRadius: AppRadius.card,
                child: Stack(
                  alignment: Alignment.center,
                  children: [
                    // Live MobileScanner View
                    Positioned.fill(
                      child: MobileScanner(
                        controller: _scannerController,
                        fit: BoxFit.cover,
                        onDetect: (BarcodeCapture capture) {
                          if (_isScannerPaused) return;
                          for (final barcode in capture.barcodes) {
                            final raw = barcode.rawValue;
                            if (raw != null && raw.isNotEmpty) {
                              _processQr(raw);
                              break;
                            }
                          }
                        },
                        errorBuilder: (context, error) {
                          return Container(
                            color: const Color(0xFF0F172A),
                            padding: AppSpacing.paddingLg,
                            child: Center(
                              child: Column(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  Icon(
                                    LucideIcons.camera_off,
                                    size: 44,
                                    color: AppColors.textMuted.withOpacity(0.7),
                                  ),
                                  const SizedBox(height: AppSpacing.sm),
                                  Text(
                                    'Camera Viewfinder Standby',
                                    style: AppTextStyles.labelLarge.copyWith(
                                      color: Colors.white,
                                      fontWeight: FontWeight.w600,
                                    ),
                                  ),
                                  const SizedBox(height: AppSpacing.xs),
                                  Text(
                                    'Point camera or use manual input below to inspect any QR payload.',
                                    textAlign: TextAlign.center,
                                    style: AppTextStyles.labelSmall.copyWith(
                                      color: AppColors.textMuted,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          );
                        },
                      ),
                    ),

                    // Cyber Target Frame overlay
                    Center(
                      child: Container(
                        width: 170,
                        height: 170,
                        decoration: BoxDecoration(
                          border: Border.all(
                            color: AppColors.primary.withOpacity(0.4),
                            width: 1.0,
                          ),
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                    ),

                    // Animated Scanning Laser Bar
                    AnimatedBuilder(
                      animation: _laserAnimation,
                      builder: (context, child) {
                        return Positioned(
                          top: 40 + (170 * _laserAnimation.value),
                          child: Container(
                            width: 170,
                            height: 2.5,
                            decoration: BoxDecoration(
                              color: AppColors.primary,
                              borderRadius: AppRadius.badge,
                              boxShadow: [
                                BoxShadow(
                                  color: AppColors.primary.withOpacity(0.75),
                                  blurRadius: 6,
                                  spreadRadius: 1,
                                ),
                              ],
                            ),
                          ),
                        );
                      },
                    ),

                    // Top Floating Camera Controls
                    Positioned(
                      top: AppSpacing.sm,
                      right: AppSpacing.sm,
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          // Torch toggle button
                          IconButton(
                            iconSize: 20,
                            style: IconButton.styleFrom(
                              backgroundColor: Colors.black.withOpacity(0.55),
                              foregroundColor: _isTorchOn ? AppColors.caution : Colors.white,
                            ),
                            icon: Icon(_isTorchOn ? LucideIcons.zap : LucideIcons.zap_off),
                            tooltip: 'Flashlight',
                            onPressed: () async {
                              await _scannerController.toggleTorch();
                              setState(() => _isTorchOn = !_isTorchOn);
                            },
                          ),
                          const SizedBox(width: AppSpacing.xs),
                          // Camera switch button
                          IconButton(
                            iconSize: 20,
                            style: IconButton.styleFrom(
                              backgroundColor: Colors.black.withOpacity(0.55),
                              foregroundColor: Colors.white,
                            ),
                            icon: const Icon(LucideIcons.switch_camera),
                            tooltip: 'Flip Camera',
                            onPressed: () => _scannerController.switchCamera(),
                          ),
                          const SizedBox(width: AppSpacing.xs),
                          // Pause / Resume button
                          IconButton(
                            iconSize: 20,
                            style: IconButton.styleFrom(
                              backgroundColor: Colors.black.withOpacity(0.55),
                              foregroundColor: Colors.white,
                            ),
                            icon: Icon(_isScannerPaused ? LucideIcons.play : LucideIcons.pause),
                            tooltip: _isScannerPaused ? 'Resume' : 'Pause',
                            onPressed: () {
                              setState(() => _isScannerPaused = !_isScannerPaused);
                            },
                          ),
                        ],
                      ),
                    ),

                    // Bottom Status Overlay
                    Positioned(
                      bottom: AppSpacing.sm,
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md, vertical: AppSpacing.xs),
                        decoration: BoxDecoration(
                          color: Colors.black.withOpacity(0.75),
                          borderRadius: AppRadius.badge,
                          border: Border.all(color: AppColors.primary.withOpacity(0.4)),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const Icon(LucideIcons.shield_check, color: AppColors.primary, size: 13),
                            const SizedBox(width: AppSpacing.xs + 2),
                            Text(
                              'Live Scanner Active • Zero Cloud • Sub-5ms',
                              style: AppTextStyles.labelSmall.copyWith(
                                color: Colors.white,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),

            const SizedBox(height: AppSpacing.lg),

            // Manual QR Input Section
            const SectionHeader(
              title: 'Manual QR Payload Testing',
              subtitle: 'Type, paste, or inspect any QR link or raw string directly',
            ),
            const SizedBox(height: AppSpacing.sm),

            InputField(
              controller: _qrTextController,
              hintText: 'Enter or paste QR text payload here...',
              prefixIcon: const Icon(LucideIcons.qr_code, color: AppColors.primary, size: 18),
              suffixIcon: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  IconButton(
                    icon: const Icon(LucideIcons.clipboard, color: AppColors.textSecondary, size: 18),
                    tooltip: 'Paste from Clipboard',
                    onPressed: _pasteFromClipboard,
                  ),
                  if (_qrTextController.text.isNotEmpty)
                    IconButton(
                      icon: const Icon(LucideIcons.x, color: AppColors.textMuted, size: 16),
                      tooltip: 'Clear',
                      onPressed: () {
                        setState(() {
                          _qrTextController.clear();
                          _decodedPayload = '';
                          _currentResult = null;
                        });
                      },
                    ),
                ],
              ),
              onSubmitted: (val) => _processQr(val),
            ),

            const SizedBox(height: AppSpacing.sm),

            Row(
              children: [
                Expanded(
                  flex: 3,
                  child: AppButton(
                    label: 'Analyze QR Payload',
                    height: 46,
                    isLoading: state.isScanning,
                    icon: const Icon(LucideIcons.shield_alert, size: 16, color: Colors.white),
                    onPressed: state.isScanning ? null : () => _processQr(_qrTextController.text),
                  ),
                ),
                const SizedBox(width: AppSpacing.sm),
                Expanded(
                  flex: 2,
                  child: AppButton.secondary(
                    label: 'Paste & Scan',
                    height: 46,
                    icon: const Icon(LucideIcons.clipboard_paste, size: 16, color: AppColors.primary),
                    onPressed: state.isScanning ? null : _pasteFromClipboard,
                  ),
                ),
              ],
            ),

            if (state.isScanning) ...[
              const SizedBox(height: AppSpacing.lg),
              const ScanningRadar(statusText: 'Decoding QR Payload & Analyzing Destination...'),
            ],

            // Decoded QR Result Display
            if (_currentResult != null && !state.isScanning) ...[
              const SizedBox(height: AppSpacing.xl),
              const SectionHeader(
                title: 'Decoded QR Threat Verdict',
                subtitle: 'Real-time on-device explainable AI analysis',
              ),
              const SizedBox(height: AppSpacing.sm),
              AppCard(
                backgroundColor: AppColors.surfaceAlt,
                child: Row(
                  children: [
                    const Icon(LucideIcons.file_text, size: 16, color: AppColors.primary),
                    const SizedBox(width: AppSpacing.sm),
                    Expanded(
                      child: SelectableText(
                        _decodedPayload,
                        style: AppTextStyles.mono.copyWith(fontSize: 12),
                      ),
                    ),
                    IconButton(
                      icon: const Icon(LucideIcons.copy, size: 16, color: AppColors.textSecondary),
                      tooltip: 'Copy Payload',
                      onPressed: () {
                        Clipboard.setData(ClipboardData(text: _decodedPayload));
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('Copied QR payload to clipboard')),
                        );
                      },
                    ),
                  ],
                ),
              ),
              const SizedBox(height: AppSpacing.md),
              XaiBreakdownCard(result: _currentResult!, animateEntrance: true),
            ],

            const SizedBox(height: AppSpacing.xl),

            // Manual Test Vectors
            const SectionHeader(
              title: 'Manual Test Vectors',
              subtitle: 'Select any threat scenario to test on-device heuristics instantly',
            ),
            const SizedBox(height: AppSpacing.sm),

            ..._manualTestVectors.map((sample) {
              return Padding(
                padding: const EdgeInsets.only(bottom: AppSpacing.sm),
                child: AppListTile(
                  title: sample['title']!,
                  subtitle: sample['payload']!,
                  leadingIcon: LucideIcons.qr_code,
                  trailing: TextButton(
                    onPressed: () {
                      _processQr(sample['payload']!);
                    },
                    child: Text(
                      'Test',
                      style: AppTextStyles.labelLarge.copyWith(
                        color: AppColors.primary,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ),
              );
            }),

            const SizedBox(height: AppSpacing.xxl),
          ],
        ),
      ),
    );
  }
}
