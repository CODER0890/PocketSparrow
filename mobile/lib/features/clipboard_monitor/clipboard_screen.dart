import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_lucide/flutter_lucide.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import '../../core/app_state_provider.dart';
import '../../core/threat_model.dart';
import '../../ui/theme/app_colors.dart';
import '../../ui/theme/app_radius.dart';
import '../../ui/theme/app_spacing.dart';
import '../../ui/theme/app_text_styles.dart';
import '../../ui/widgets/app_button.dart';
import '../../ui/widgets/app_card.dart';
import '../../ui/widgets/app_chip.dart';
import '../../ui/widgets/app_divider.dart';
import '../../ui/widgets/app_list_tile.dart';
import '../../ui/widgets/app_scaffold.dart';
import '../../ui/widgets/empty_state.dart';
import '../../ui/widgets/section_header.dart';
import '../../ui/widgets/verdict_badge.dart';
import '../../ui/widgets/xai_breakdown_card.dart';
import '../../ui/widgets/app_switch.dart';

class ClipboardScreen extends StatefulWidget {
  const ClipboardScreen({super.key});

  @override
  State<ClipboardScreen> createState() => _ClipboardScreenState();
}

class _ClipboardScreenState extends State<ClipboardScreen> {
  ThreatResult? _inspectedResult;

  String _formatTime(DateTime dt) {
    return DateFormat('HH:mm:ss').format(dt);
  }

  void _simulateMaliciousCopy(BuildContext context, String payload) async {
    await Clipboard.setData(ClipboardData(text: payload));
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Simulated copied text: "$payload"'),
          duration: const Duration(seconds: 2),
        ),
      );
    }
    if (!context.mounted) return;
    final state = context.read<AppStateProvider>();
    final rec = await state.clipboardService.checkNow();
    if (mounted) {
      setState(() {
        if (rec != null) {
          _inspectedResult = rec.result;
        }
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppStateProvider>();
    final history = state.clipboardService.history;

    return AppScaffold(
      title: 'Clipboard Shield',
      actions: [
        IconButton(
          icon: const Icon(LucideIcons.trash, color: AppColors.textMuted, size: 20),
          tooltip: 'Clear Feed',
          onPressed: () {
            state.clipboardService.clearHistory();
            setState(() {
              _inspectedResult = null;
            });
          },
        ),
      ],
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Status Card
            AppCard(
              child: Column(
                children: [
                  Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(AppSpacing.md),
                        decoration: BoxDecoration(
                          color: (state.isClipboardMonitoring
                                  ? AppColors.primary
                                  : AppColors.textMuted)
                              .withOpacity(0.12),
                          borderRadius: AppRadius.borderMd,
                        ),
                        child: Icon(
                          LucideIcons.clipboard_check,
                          color: state.isClipboardMonitoring
                              ? AppColors.primary
                              : AppColors.textMuted,
                          size: 22,
                        ),
                      ),
                      const SizedBox(width: AppSpacing.md),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              state.isClipboardMonitoring
                                  ? 'Clipboard Guardian Active'
                                  : 'Clipboard Monitoring Disabled',
                              style: AppTextStyles.labelLarge.copyWith(
                                fontWeight: FontWeight.w700,
                                color: AppColors.textPrimary,
                              ),
                            ),
                            const SizedBox(height: AppSpacing.xs / 2),
                            Text(
                              'Silently checks newly copied links for phishing, punycode and cloaks in volatile memory.',
                              style: AppTextStyles.bodyMedium.copyWith(
                                color: AppColors.textSecondary,
                                fontSize: 13,
                              ),
                            ),
                          ],
                        ),
                      ),
                      AppSwitch(
                        value: state.isClipboardMonitoring,
                        onChanged: (val) {
                          state.toggleClipboardMonitoring(val);
                        },
                      ),
                    ],
                  ),
                  const AppDivider(verticalMargin: AppSpacing.md),
                  AppButton(
                    label: 'Check Current Clipboard Now',
                    icon: LucideIcons.refresh_cw,
                    variant: AppButtonVariant.outline,
                    isFullWidth: true,
                    onPressed: () async {
                      final rec = await state.clipboardService.checkNow();
                      if (rec != null) {
                        setState(() {
                          _inspectedResult = rec.result;
                        });
                      } else {
                        if (context.mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('Clipboard unchanged or empty')),
                          );
                        }
                      }
                    },
                  ),
                ],
              ),
            ),

            // Inspected result if selected
            if (_inspectedResult != null) ...[
              const SizedBox(height: AppSpacing.lg),
              const SectionHeader(
                title: 'LATEST THREAT BREAKDOWN',
                subtitle: 'Explainable AI heuristic breakdown of selected entry',
              ),
              const SizedBox(height: AppSpacing.sm),
              XaiBreakdownCard(result: _inspectedResult!),
            ],

            const SizedBox(height: AppSpacing.xl),

            // Manual Clipboard Test Suite
            const SectionHeader(
              title: 'MANUAL CLIPBOARD TEST SUITE',
              subtitle: 'Inspect device clipboard or simulate clipboard injection attacks',
            ),
            const SizedBox(height: AppSpacing.sm),

            Row(
              children: [
                Expanded(
                  child: AppButton.secondary(
                    label: 'Inspect Device Clipboard Now',
                    height: 42,
                    icon: const Icon(LucideIcons.clipboard, size: 16, color: AppColors.primary),
                    onPressed: () async {
                      final state = context.read<AppStateProvider>();
                      final data = await Clipboard.getData(Clipboard.kTextPlain);
                      final text = data?.text?.trim() ?? '';
                      if (text.isEmpty) {
                        if (context.mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('Clipboard is currently empty')),
                          );
                        }
                        return;
                      }
                      final res = await state.scanClipboardDirect(text);
                      if (mounted) {
                        setState(() => _inspectedResult = res);
                      }
                    },
                  ),
                ),
              ],
            ),
            const SizedBox(height: AppSpacing.sm),

            Wrap(
              spacing: AppSpacing.sm,
              runSpacing: AppSpacing.sm,
              children: [
                AppChip(
                  leading: const Icon(LucideIcons.shield_alert, size: 14, color: AppColors.blocked),
                  label: 'Phishing Link',
                  isSelected: false,
                  onTap: () => _simulateMaliciousCopy(
                    context,
                    'https://paypal-security-update.xyz/verify',
                  ),
                ),
                AppChip(
                  leading: const Icon(LucideIcons.shield_alert, size: 14, color: AppColors.blocked),
                  label: 'Cyrillic Lookalike',
                  isSelected: false,
                  onTap: () => _simulateMaliciousCopy(
                    context,
                    'https://\u0430pple.com/auth-id',
                  ),
                ),
                AppChip(
                  leading: const Icon(LucideIcons.shield_check, size: 14, color: AppColors.safe),
                  label: 'Safe URL',
                  isSelected: false,
                  onTap: () => _simulateMaliciousCopy(
                    context,
                    'https://google.com',
                  ),
                ),
              ],
            ),

            // Live Feed Section
            const SectionHeader(
              title: 'LIVE CLIPBOARD INSPECTION FEED',
              subtitle: 'Real-time log of examined clipboard content',
            ),
            const SizedBox(height: AppSpacing.sm),

            if (history.isEmpty)
              const EmptyState(
                icon: LucideIcons.clipboard_list,
                title: 'No clipboard activity yet',
                description: 'Copy any text or link on your device to inspect it automatically.',
              )
            else
              Column(
                children: history.map((rec) {
                  return Padding(
                    padding: const EdgeInsets.only(bottom: AppSpacing.sm),
                    child: AppListTile(
                      onTap: () {
                        setState(() {
                          _inspectedResult = rec.result;
                        });
                      },
                      leading: Container(
                        padding: const EdgeInsets.all(AppSpacing.sm),
                        decoration: BoxDecoration(
                          color: rec.result.isSafe
                              ? AppColors.safeLight
                              : (rec.result.isCaution ? AppColors.cautionLight : AppColors.blockedLight),
                          borderRadius: AppRadius.borderSm,
                        ),
                        child: Icon(
                          rec.result.isSafe
                              ? LucideIcons.shield_check
                              : (rec.result.isCaution ? LucideIcons.triangle_alert : LucideIcons.shield_x),
                          color: rec.result.isSafe
                              ? AppColors.safe
                              : (rec.result.isCaution ? AppColors.caution : AppColors.blocked),
                          size: 18,
                        ),
                      ),
                      title: Text(
                        rec.content,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: AppTextStyles.labelLarge.copyWith(fontWeight: FontWeight.w600),
                      ),
                      subtitle: Text(
                        'Captured ${_formatTime(rec.timestamp)} • ${rec.result.latencyMs}ms',
                        style: AppTextStyles.labelSmall.copyWith(color: AppColors.textMuted),
                      ),
                      trailing: VerdictBadge(verdict: rec.result.verdict),
                    ),
                  );
                }).toList(),
              ),

            const SizedBox(height: AppSpacing.xxl),
          ],
        ),
      ),
    );
  }
}
