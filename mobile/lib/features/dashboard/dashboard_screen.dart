import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_lucide/flutter_lucide.dart';
import 'package:provider/provider.dart';
import '../../core/app_state_provider.dart';
import '../../core/threat_model.dart';
import '../../ui/theme/app_colors.dart';
import '../../ui/theme/app_radius.dart';
import '../../ui/theme/app_spacing.dart';
import '../../ui/theme/app_text_styles.dart';
import '../../ui/widgets/app_button.dart';
import '../../ui/widgets/app_card.dart';
import '../../ui/widgets/app_divider.dart';
import '../../ui/widgets/app_list_tile.dart';
import '../../ui/widgets/empty_state.dart';
import '../../ui/widgets/network_status_badge.dart';
import '../../ui/widgets/pulse_shield.dart';
import '../../ui/widgets/section_header.dart';
import '../../ui/widgets/stat_tile.dart';
import '../../ui/widgets/verdict_badge.dart';
import '../../ui/widgets/xai_reason_tile.dart';
import '../../ui/widgets/app_switch.dart';

class DashboardScreen extends StatelessWidget {
  final Function(int) onNavigateToTab;

  const DashboardScreen({super.key, required this.onNavigateToTab});

  String _formatTime(DateTime dt) {
    final diff = DateTime.now().difference(dt);
    if (diff.inMinutes < 1) return 'Just now';
    if (diff.inMinutes < 60) return '${diff.inMinutes}m ago';
    if (diff.inHours < 24) return '${diff.inHours}h ago';
    return '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
  }

  IconData _getTypeIcon(ThreatType type) {
    switch (type) {
      case ThreatType.url:
        return LucideIcons.link;
      case ThreatType.sms:
        return LucideIcons.message_square;
      case ThreatType.qr:
        return LucideIcons.qr_code;
      case ThreatType.clipboard:
        return LucideIcons.clipboard;
    }
  }

  void _showThreatDetail(BuildContext context, ThreatResult threat) {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.surface,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) {
        return DraggableScrollableSheet(
          expand: false,
          initialChildSize: 0.65,
          minChildSize: 0.4,
          maxChildSize: 0.9,
          builder: (_, scrollController) {
            return Padding(
              padding: const EdgeInsets.symmetric(
                horizontal: AppSpacing.lg,
                vertical: AppSpacing.md,
              ),
              child: ListView(
                controller: scrollController,
                children: [
                  Center(
                    child: Container(
                      width: 36,
                      height: 4,
                      decoration: BoxDecoration(
                        color: AppColors.border,
                        borderRadius: AppRadius.badge,
                      ),
                    ),
                  ),
                  const SizedBox(height: AppSpacing.lg),
                  Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(AppSpacing.sm),
                        decoration: BoxDecoration(
                          color: AppColors.primaryLight,
                          shape: BoxShape.circle,
                        ),
                        child: Icon(
                          _getTypeIcon(threat.type),
                          color: AppColors.primary,
                          size: 18,
                        ),
                      ),
                      const SizedBox(width: AppSpacing.md),
                      Expanded(
                        child: Text(
                          '${threat.type.displayName} Inspection',
                          style: AppTextStyles.headingMedium,
                        ),
                      ),
                      VerdictBadge(verdict: threat.verdict, isLarge: true),
                    ],
                  ),
                  const SizedBox(height: AppSpacing.md),
                  Container(
                    width: double.infinity,
                    padding: AppSpacing.paddingMd,
                    decoration: BoxDecoration(
                      color: AppColors.surfaceAlt,
                      borderRadius: AppRadius.button,
                      border: Border.all(color: AppColors.border, width: 1.0),
                    ),
                    child: SelectableText(
                      threat.rawInput,
                      style: AppTextStyles.mono,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.lg),
                  SectionHeader(title: 'Explainable AI Reasons (${threat.reasons.length})'),
                  const SizedBox(height: AppSpacing.sm),
                  ...threat.reasons.map(
                    (reason) => Padding(
                      padding: const EdgeInsets.only(bottom: AppSpacing.sm),
                      child: XaiReasonTile(
                        text: reason,
                        icon: threat.isSafe
                            ? LucideIcons.shield_check
                            : (threat.isCaution ? LucideIcons.triangle_alert : LucideIcons.shield_x),
                        iconColor: threat.isSafe
                            ? AppColors.safe
                            : (threat.isCaution ? AppColors.caution : AppColors.blocked),
                      ),
                    ),
                  ),
                  const SizedBox(height: AppSpacing.lg),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        'Latency: ${threat.latencyMs} ms',
                        style: AppTextStyles.mono.copyWith(color: AppColors.textMuted),
                      ),
                      Text(
                        'Time: ${_formatTime(threat.timestamp)}',
                        style: AppTextStyles.labelSmall,
                      ),
                    ],
                  ),
                  const SizedBox(height: AppSpacing.xl),
                ],
              ),
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppStateProvider>();
    final stats = state.stats;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: PreferredSize(
        preferredSize: const Size.fromHeight(kToolbarHeight),
        child: Container(
          decoration: const BoxDecoration(
            color: AppColors.surface,
            border: Border(
              bottom: BorderSide(color: AppColors.border, width: 1.0),
            ),
          ),
          child: AppBar(
            backgroundColor: AppColors.surface,
            elevation: 0,
            scrolledUnderElevation: 0,
            centerTitle: false,
            title: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(AppSpacing.sm),
                  decoration: BoxDecoration(
                    color: AppColors.primaryLight,
                    borderRadius: AppRadius.button,
                  ),
                  child: const Icon(LucideIcons.shield_check, color: AppColors.primary, size: 20),
                ),
                const SizedBox(width: AppSpacing.sm),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        'Pocket Sparrow',
                        style: AppTextStyles.headingMedium.copyWith(fontSize: 16),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      Text(
                        'Zero-Cloud Local Threat Shield',
                        style: AppTextStyles.labelSmall.copyWith(fontSize: 11),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ),
                ),
              ],
            ),
            actions: const [
              Padding(
                padding: EdgeInsets.only(right: AppSpacing.lg),
                child: NetworkStatusBadge(),
              ),
            ],
          ),
        ),
      ),
      body: RefreshIndicator(
        color: AppColors.primary,
        onRefresh: () => state.refreshData(),
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: AppSpacing.paddingLg,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 1. Big Status AppCard (safeLight bg, shield icon, "Protection Active")
              AppCard(
                backgroundColor: state.isProtectionActive ? AppColors.safeLight : AppColors.surfaceAlt,
                borderColor: state.isProtectionActive ? AppColors.safe.withOpacity(0.3) : AppColors.border,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.center,
                      children: [
                        PulseShield(isActive: state.isProtectionActive, size: 52),
                        const SizedBox(width: AppSpacing.lg),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  Flexible(
                                    child: Text(
                                      state.isProtectionActive ? 'Protection Active' : 'Protection Paused',
                                      style: AppTextStyles.headingMedium.copyWith(
                                        color: state.isProtectionActive ? AppColors.safe : AppColors.textPrimary,
                                        fontWeight: FontWeight.w700,
                                      ),
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ),
                                  const SizedBox(width: AppSpacing.sm),
                                  Container(
                                    width: 8,
                                    height: 8,
                                    decoration: BoxDecoration(
                                      shape: BoxShape.circle,
                                      color: state.isProtectionActive ? AppColors.safe : AppColors.blocked,
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: AppSpacing.xs),
                              Text(
                                state.isProtectionActive
                                    ? 'On-device neural & heuristic sensors monitoring memory, links & SMS.'
                                    : 'Local threat detection is currently paused.',
                                style: AppTextStyles.bodyMedium,
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: AppSpacing.lg),
                    const AppDivider(),
                    const SizedBox(height: AppSpacing.md),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Expanded(
                          child: Row(
                            children: [
                              Icon(
                                LucideIcons.clipboard,
                                size: 18,
                                color: state.isClipboardMonitoring ? AppColors.primary : AppColors.textMuted,
                              ),
                              const SizedBox(width: AppSpacing.sm),
                              Flexible(
                                child: Text(
                                  'Auto Clipboard Shield',
                                  style: AppTextStyles.labelLarge.copyWith(fontWeight: FontWeight.w600),
                                  overflow: TextOverflow.ellipsis,
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
                  ],
                ),
              ),

              const SizedBox(height: AppSpacing.lg),

              // 2. Row of 3 StatTiles: Threats Blocked / Avg Check / Cloud Calls (0)
              Row(
                children: [
                  Expanded(
                    child: StatTile(
                      label: 'Threats Blocked',
                      value: '${stats['blockedToday'] ?? 0}',
                      icon: LucideIcons.shield_x,
                      iconColor: AppColors.blocked,
                      subtitle: '${stats['total'] ?? 0} inspected',
                    ),
                  ),
                  const SizedBox(width: AppSpacing.sm),
                  Expanded(
                    child: StatTile(
                      label: 'Avg Check',
                      value: '${stats['avgLatencyMs'] ?? 3} ms',
                      icon: LucideIcons.zap,
                      iconColor: AppColors.primary,
                      subtitle: '<5ms target',
                    ),
                  ),
                  const SizedBox(width: AppSpacing.sm),
                  Expanded(
                    child: StatTile(
                      label: 'Cloud Calls (0)',
                      value: '0',
                      icon: LucideIcons.wifi_off,
                      iconColor: AppColors.safe,
                      subtitle: 'Zero egress',
                    ),
                  ),
                ],
              ),

              const SizedBox(height: AppSpacing.lg),

              // 3. Quick Actions Grid: 4 AppButtons (Scan URL, Scan SMS, Scan QR, Paste)
              const SectionHeader(title: 'Quick Actions'),
              const SizedBox(height: AppSpacing.sm),

              Row(
                children: [
                  Expanded(
                    child: AppButton(
                      label: 'Scan URL',
                      icon: const Icon(LucideIcons.link, size: 18, color: Colors.white),
                      onPressed: () => onNavigateToTab(1),
                    ),
                  ),
                  const SizedBox(width: AppSpacing.md),
                  Expanded(
                    child: AppButton(
                      label: 'Scan SMS',
                      icon: const Icon(LucideIcons.message_square, size: 18, color: Colors.white),
                      onPressed: () => onNavigateToTab(2),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: AppSpacing.md),
              Row(
                children: [
                  Expanded(
                    child: AppButton(
                      label: 'Scan QR',
                      icon: const Icon(LucideIcons.qr_code, size: 18, color: Colors.white),
                      onPressed: () => onNavigateToTab(3),
                    ),
                  ),
                  const SizedBox(width: AppSpacing.md),
                  Expanded(
                    child: AppButton.secondary(
                      label: 'Paste & Check',
                      icon: const Icon(LucideIcons.clipboard, size: 18, color: AppColors.primary),
                      onPressed: () async {
                        final data = await Clipboard.getData(Clipboard.kTextPlain);
                        final text = data?.text?.trim();
                        if (text == null || text.isEmpty) {
                          if (context.mounted) {
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(content: Text('Clipboard is empty')),
                            );
                          }
                          return;
                        }
                        await state.scanClipboardDirect(text);
                        if (context.mounted) {
                          onNavigateToTab(4);
                        }
                      },
                    ),
                  ),
                ],
              ),

              const SizedBox(height: AppSpacing.lg),

              // 4. Recent Threats Section
              SectionHeader(
                title: 'Recent Threats',
                trailing: TextButton(
                  onPressed: () => onNavigateToTab(5),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        'View All',
                        style: AppTextStyles.labelSmall.copyWith(
                          color: AppColors.primary,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(width: AppSpacing.xs),
                      const Icon(LucideIcons.chevron_right, size: 14, color: AppColors.primary),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.sm),

              if (state.recentThreats.isEmpty)
                const EmptyState(
                  icon: LucideIcons.shield_check,
                  title: 'No Threats Detected',
                  description: 'All inspections are clear. Scan a link or message to begin.',
                )
              else
                ...state.recentThreats.take(5).map((threat) {
                  return Padding(
                    padding: const EdgeInsets.only(bottom: AppSpacing.sm),
                    child: AppListTile(
                      leadingIcon: _getTypeIcon(threat.type),
                      title: threat.rawInput.trim(),
                      subtitle: '${_formatTime(threat.timestamp)} • ${threat.latencyMs}ms',
                      trailing: VerdictBadge(verdict: threat.verdict),
                      onTap: () => _showThreatDetail(context, threat),
                    ),
                  );
                }),

              const SizedBox(height: AppSpacing.xl),
            ],
          ),
        ),
      ),
    );
  }
}
