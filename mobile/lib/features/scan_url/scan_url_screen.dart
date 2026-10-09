import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_lucide/flutter_lucide.dart';
import 'package:provider/provider.dart';
import '../../core/app_state_provider.dart';
import '../../core/threat_model.dart';
import '../../ui/theme/app_colors.dart';
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

class ScanUrlScreen extends StatefulWidget {
  const ScanUrlScreen({super.key});

  @override
  State<ScanUrlScreen> createState() => _ScanUrlScreenState();
}

class _ScanUrlScreenState extends State<ScanUrlScreen> {
  final TextEditingController _urlController = TextEditingController();
  ThreatResult? _currentResult;

  final List<Map<String, String>> _sampleLinks = [
    {
      'label': 'Cyrillic Apple (Homoglyph)',
      'url': 'https://\u0430pple.com/login',
      'tag': 'BLOCKED',
    },
    {
      'label': 'PayPal Phish (.xyz)',
      'url': 'https://paypal-security-update.xyz/verify',
      'tag': 'BLOCKED',
    },
    {
      'label': 'Bitly Shortener Cloak',
      'url': 'https://bit.ly/fake-bank',
      'tag': 'CAUTION',
    },
    {
      'label': 'Google (Authentic)',
      'url': 'https://google.com',
      'tag': 'SAFE',
    },
    {
      'label': 'IP Address Host',
      'url': 'http://192.168.1.100/secure-update',
      'tag': 'BLOCKED',
    },
  ];

  @override
  void dispose() {
    _urlController.dispose();
    super.dispose();
  }

  Future<void> _analyze(String input) async {
    final trimmed = input.trim();
    if (trimmed.isEmpty) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Please enter a URL to analyze')),
        );
      }
      return;
    }

    try {
      final state = context.read<AppStateProvider>();
      final result = await state.scanUrl(trimmed);
      if (mounted) {
        setState(() {
          _currentResult = result;
        });
      }
    } catch (e) {
      debugPrint('Error analyzing URL: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppStateProvider>();

    return AppScaffold(
      title: Text(
        'Scan URL Link',
        style: AppTextStyles.headingLarge,
      ),
      body: SingleChildScrollView(
        padding: AppSpacing.paddingLg,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Top Info Banner: primaryLight background, text primaryText, icon Info
            AppCard(
              backgroundColor: AppColors.primaryLight,
              borderColor: AppColors.primary.withOpacity(0.2),
              padding: AppSpacing.paddingMd,
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(LucideIcons.info, color: AppColors.primaryText, size: 18),
                  const SizedBox(width: AppSpacing.md),
                  Expanded(
                    child: Text(
                      'Evaluates domain Shannon entropy, Cyrillic lookalikes, brand spoofing, and IP origins strictly on-device in <5ms.',
                      style: AppTextStyles.bodyMedium.copyWith(
                        color: AppColors.primaryText,
                        fontSize: 13,
                        height: 1.4,
                      ),
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: AppSpacing.lg),

            // Input Field: InputField widget
            InputField(
              controller: _urlController,
              keyboardType: TextInputType.url,
              hintText: 'Enter URL (e.g. https://example.com)',
              prefixIcon: const Icon(LucideIcons.link, color: AppColors.primary, size: 18),
              suffixIcon: IconButton(
                icon: const Icon(LucideIcons.clipboard, color: AppColors.textSecondary, size: 18),
                tooltip: 'Paste from Clipboard',
                onPressed: () async {
                  final data = await Clipboard.getData(Clipboard.kTextPlain);
                  if (data?.text != null) {
                    _urlController.text = data!.text!.trim();
                  }
                },
              ),
              onSubmitted: (val) => _analyze(val),
            ),

            const SizedBox(height: AppSpacing.md),

            // Primary Analyze Button: AppButton widget
            AppButton(
              label: 'Check Link (<5ms)',
              height: 52,
              isLoading: state.isScanning,
              icon: const Icon(LucideIcons.search, size: 18, color: Colors.white),
              onPressed: state.isScanning ? null : () => _analyze(_urlController.text),
            ),

            if (state.isScanning) ...[
              const SizedBox(height: AppSpacing.md),
              const ScanningRadar(statusText: 'Inspecting Entropy, Homoglyphs & Brand TLDs...'),
            ],

            // Detection Result & Score: Displayed prominently right beneath the analyze button
            if (_currentResult != null && !state.isScanning) ...[
              const SizedBox(height: AppSpacing.lg),
              const SectionHeader(title: 'Detection Result & Reasons'),
              const SizedBox(height: AppSpacing.sm),
              XaiBreakdownCard(result: _currentResult!, animateEntrance: true),
            ],

            // Manual Test Vectors
            const SectionHeader(
              title: 'Manual Test Vectors',
              subtitle: 'Select any threat scenario to test on-device heuristics instantly',
            ),
            const SizedBox(height: AppSpacing.sm),

            ..._sampleLinks.map((sample) {
              return Padding(
                padding: const EdgeInsets.only(bottom: AppSpacing.sm),
                child: AppListTile(
                  title: sample['label']!,
                  subtitle: sample['url']!,
                  leadingIcon: LucideIcons.link,
                  trailing: TextButton(
                    onPressed: () {
                      _urlController.text = sample['url']!;
                      _analyze(sample['url']!);
                    },
                    child: Text(
                      'Load',
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
