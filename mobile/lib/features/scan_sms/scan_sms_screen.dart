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
import '../../ui/widgets/app_divider.dart';
import '../../ui/widgets/app_list_tile.dart';
import '../../ui/widgets/app_scaffold.dart';
import '../../ui/widgets/input_field.dart';
import '../../ui/widgets/scanning_radar.dart';
import '../../ui/widgets/section_header.dart';
import '../../ui/widgets/xai_breakdown_card.dart';

class ScanSmsScreen extends StatefulWidget {
  const ScanSmsScreen({super.key});

  @override
  State<ScanSmsScreen> createState() => _ScanSmsScreenState();
}

class _ScanSmsScreenState extends State<ScanSmsScreen> {
  final TextEditingController _smsController = TextEditingController();
  ThreatResult? _currentResult;

  final List<Map<String, String>> _manualTestScenarios = [
    {
      'title': 'SBI Account Block Scam',
      'body': 'URGENT: Your SBI account will be blocked. Verify now at http://sbi-kyc-update.xyz',
      'tag': 'BLOCKED',
    },
    {
      'title': 'Electricity Cutoff Fraud',
      'body': 'Dear consumer, electricity power will be disconnected tonight 9:30pm due to unpaid bill ₹1450. Contact officer immediately at 9876543210 or bit.ly/power-pay',
      'tag': 'BLOCKED',
    },
    {
      'title': 'Lottery / Cashback Bait',
      'body': 'Congratulations! You won cash prize ₹50,000 lottery reward. Claim immediately at http://free-giftcard-claim.buzz',
      'tag': 'BLOCKED',
    },
    {
      'title': 'Safe Conversational SMS',
      'body': 'Hi John, the team meeting has been rescheduled to tomorrow 10am in conference room B. See you there!',
      'tag': 'SAFE',
    },
  ];

  @override
  void dispose() {
    _smsController.dispose();
    super.dispose();
  }

  Future<void> _analyze(String text) async {
    final trimmed = text.trim();
    if (trimmed.isEmpty) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Please enter an SMS or text message to analyze')),
        );
      }
      return;
    }

    try {
      final state = context.read<AppStateProvider>();
      final result = await state.scanSms(trimmed);
      if (mounted) {
        setState(() {
          _currentResult = result;
        });
      }
    } catch (e) {
      debugPrint('Error analyzing SMS: $e');
    }
  }

  Widget _buildHighlightedMessage(String fullText, List<String> flaggedTokens) {
    if (flaggedTokens.isEmpty) {
      return Text(
        fullText,
        style: AppTextStyles.bodyMedium.copyWith(color: AppColors.textPrimary),
      );
    }

    final spans = <TextSpan>[];
    final escapedTokens = flaggedTokens.map((t) => RegExp.escape(t)).toList();
    final combinedPattern = RegExp('(${escapedTokens.join('|')})', caseSensitive: false);

    final matches = combinedPattern.allMatches(fullText).toList();
    if (matches.isEmpty) {
      return Text(
        fullText,
        style: AppTextStyles.bodyMedium.copyWith(color: AppColors.textPrimary),
      );
    }

    int currentIndex = 0;
    for (final match in matches) {
      if (match.start > currentIndex) {
        spans.add(TextSpan(
          text: fullText.substring(currentIndex, match.start),
          style: AppTextStyles.bodyMedium.copyWith(color: AppColors.textPrimary),
        ));
      }
      spans.add(TextSpan(
        text: fullText.substring(match.start, match.end),
        style: AppTextStyles.bodyMedium.copyWith(
          color: AppColors.blocked,
          backgroundColor: AppColors.blockedLight,
          fontWeight: FontWeight.w700,
        ),
      ));
      currentIndex = match.end;
    }

    if (currentIndex < fullText.length) {
      spans.add(TextSpan(
        text: fullText.substring(currentIndex),
        style: AppTextStyles.bodyMedium.copyWith(color: AppColors.textPrimary),
      ));
    }

    return RichText(text: TextSpan(children: spans));
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppStateProvider>();

    return AppScaffold(
      title: Text(
        'Scan SMS & Messages',
        style: AppTextStyles.headingLarge,
      ),
      body: SingleChildScrollView(
        padding: AppSpacing.paddingLg,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Top Info Banner: primaryLight (#DBEAFE) background, text #1E40AF, icon Info, radius 12
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
                      'Detects artificial urgency, institutional impersonation (banks/utilities), financial lures, and embedded malicious links without cloud access.',
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

            // Text Input: InputField with background #F1F5F9, border 1px #E2E8F0, radius 12
            InputField(
              controller: _smsController,
              maxLines: 4,
              minLines: 3,
              hintText: 'Paste suspicious SMS message here...',
              suffixIcon: IconButton(
                icon: const Icon(LucideIcons.clipboard, color: AppColors.textSecondary, size: 18),
                tooltip: 'Paste from Clipboard',
                onPressed: () async {
                  final data = await Clipboard.getData(Clipboard.kTextPlain);
                  if (data?.text != null) {
                    _smsController.text = data!.text!.trim();
                  }
                },
              ),
            ),

            const SizedBox(height: AppSpacing.md),

            // Primary Button: background #2563EB, text white w600 16sp, radius 12, height 52
            AppButton(
              label: 'Analyze Message (<5ms)',
              height: 52,
              isLoading: state.isScanning,
              onPressed: state.isScanning ? null : () => _analyze(_smsController.text),
            ),

            // Scanning state
            if (state.isScanning) ...[
              const SizedBox(height: AppSpacing.md),
              const ScanningRadar(statusText: 'Analyzing Urgency, Bank Patterns & Links...'),
            ],

            // Results Section: Prominently displayed right under the analyze button
            if (_currentResult != null && !state.isScanning) ...[
              const SizedBox(height: AppSpacing.lg),
              const SectionHeader(title: 'Flagged Message Highlights'),
              const SizedBox(height: AppSpacing.sm),

              AppCard(
                backgroundColor: AppColors.surfaceAlt,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _buildHighlightedMessage(
                      _currentResult!.rawInput,
                      _currentResult!.flaggedTokens,
                    ),
                    if (_currentResult!.flaggedTokens.isNotEmpty) ...[
                      const SizedBox(height: AppSpacing.md),
                      const AppDivider(),
                      const SizedBox(height: AppSpacing.sm),
                      Text(
                        'Flagged triggers: ${_currentResult!.flaggedTokens.join(', ')}',
                        style: AppTextStyles.labelSmall.copyWith(
                          fontStyle: FontStyle.italic,
                          color: AppColors.textMuted,
                        ),
                      ),
                    ],
                  ],
                ),
              ),

              const SizedBox(height: AppSpacing.md),
              XaiBreakdownCard(result: _currentResult!, animateEntrance: true),
            ],

            // Sample Scenarios List: AppListTile for each scenario
            const SectionHeader(
              title: 'Manual Test Scenarios',
              subtitle: 'Select any SMS scam scenario to evaluate fraud heuristics instantly',
            ),
            const SizedBox(height: AppSpacing.sm),

            ..._manualTestScenarios.map((demo) {
              return Padding(
                padding: const EdgeInsets.only(bottom: AppSpacing.sm),
                child: AppListTile(
                  title: demo['title']!,
                  subtitle: demo['body']!,
                  leadingIcon: LucideIcons.message_square,
                  trailing: TextButton(
                    onPressed: () {
                      _smsController.text = demo['body']!;
                      _analyze(demo['body']!);
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
