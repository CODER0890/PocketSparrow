import 'package:flutter/material.dart';
import 'package:flutter_lucide/flutter_lucide.dart';
import 'package:provider/provider.dart';
import '../../core/app_state_provider.dart';
import '../../ui/theme/app_colors.dart';
import '../../ui/theme/app_radius.dart';
import '../../ui/theme/app_spacing.dart';
import '../../ui/theme/app_text_styles.dart';
import '../../ui/widgets/app_button.dart';
import '../../ui/widgets/app_card.dart';
import '../../ui/widgets/app_divider.dart';
import '../../ui/widgets/app_list_tile.dart';
import '../../ui/widgets/app_scaffold.dart';
import '../../ui/widgets/section_header.dart';
import '../../ui/widgets/app_switch.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppStateProvider>();

    return AppScaffold(
      title: 'Settings & Configuration',
      body: ListView(
        padding: const EdgeInsets.all(AppSpacing.lg),
        children: [
          // Section: Defense Sensors
          const SectionHeader(
            title: 'DETECTION CONTROLS',
            subtitle: 'On-device engine and sensor preferences',
          ),
          const SizedBox(height: AppSpacing.sm),

          AppCard(
            padding: EdgeInsets.zero,
            child: Column(
              children: [
                AppSwitchListTile(
                  secondary: const Icon(LucideIcons.shield_check, color: AppColors.primary, size: 20),
                  title: Text(
                    'Master Protection Engine',
                    style: AppTextStyles.labelLarge.copyWith(fontWeight: FontWeight.w600),
                  ),
                  subtitle: Text(
                    'Enables on-device heuristics & scanners',
                    style: AppTextStyles.labelSmall.copyWith(color: AppColors.textSecondary),
                  ),
                  value: state.isProtectionActive,
                  onChanged: (val) => state.toggleProtection(val),
                ),
                const AppDivider(verticalMargin: 0),
                AppSwitchListTile(
                  secondary: const Icon(LucideIcons.clipboard_check, color: AppColors.primary, size: 20),
                  title: Text(
                    'Background Clipboard Watcher',
                    style: AppTextStyles.labelLarge.copyWith(fontWeight: FontWeight.w600),
                  ),
                  subtitle: Text(
                    'Automatically flags copied phishing links',
                    style: AppTextStyles.labelSmall.copyWith(color: AppColors.textSecondary),
                  ),
                  value: state.isClipboardMonitoring,
                  onChanged: (val) => state.toggleClipboardMonitoring(val),
                ),
                const AppDivider(verticalMargin: 0),
                AppSwitchListTile(
                  secondary: const Icon(LucideIcons.vibrate, color: AppColors.primary, size: 20),
                  title: Text(
                    'Haptic Vibration Alerts',
                    style: AppTextStyles.labelLarge.copyWith(fontWeight: FontWeight.w600),
                  ),
                  subtitle: Text(
                    'Vibrate upon malicious threat detection',
                    style: AppTextStyles.labelSmall.copyWith(color: AppColors.textSecondary),
                  ),
                  value: state.isHapticsEnabled,
                  onChanged: (val) => state.toggleHaptics(val),
                ),
              ],
            ),
          ),

          const SizedBox(height: AppSpacing.xl),

          // Section: Engine Configuration & Operations
          const SectionHeader(
            title: 'OPERATING MODE & DIAGNOSTICS',
            subtitle: 'Engine configuration and evaluation modes',
          ),
          const SizedBox(height: AppSpacing.sm),

          AppCard(
            padding: EdgeInsets.zero,
            child: Column(
              children: [
                AppListTile(
                  leading: Container(
                    padding: const EdgeInsets.all(AppSpacing.sm),
                    decoration: const BoxDecoration(
                      color: AppColors.safeLight,
                      borderRadius: AppRadius.borderSm,
                    ),
                    child: const Icon(LucideIcons.shield_check, color: AppColors.safe, size: 18),
                  ),
                  title: Text(
                    'Production Security Mode',
                    style: AppTextStyles.labelLarge.copyWith(fontWeight: FontWeight.w600),
                  ),
                  subtitle: Text(
                    'Active — Pure manual & sensor testing with zero mockups',
                    style: AppTextStyles.labelSmall.copyWith(color: AppColors.safe),
                  ),
                  trailing: Container(
                    padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm, vertical: AppSpacing.xs),
                    decoration: BoxDecoration(
                      color: AppColors.safeLight,
                      borderRadius: AppRadius.badge,
                    ),
                    child: Text(
                      'ACTIVE',
                      style: AppTextStyles.labelSmall.copyWith(
                        color: AppColors.safe,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ),
                const AppDivider(verticalMargin: 0),
                AppListTile(
                  leading: Container(
                    padding: const EdgeInsets.all(AppSpacing.sm),
                    decoration: const BoxDecoration(
                      color: AppColors.primaryLight,
                      borderRadius: AppRadius.borderSm,
                    ),
                    child: const Icon(LucideIcons.refresh_cw, color: AppColors.primary, size: 18),
                  ),
                  title: Text(
                    'Reset to Clean Production State',
                    style: AppTextStyles.labelLarge.copyWith(fontWeight: FontWeight.w600),
                  ),
                  subtitle: Text(
                    'Clears mock preloads and restores default production monitoring',
                    style: AppTextStyles.labelSmall.copyWith(color: AppColors.textSecondary),
                  ),
                  trailing: const Icon(LucideIcons.chevron_right, color: AppColors.textMuted, size: 18),
                  onTap: () async {
                    await state.resetToProductionState();
                    if (context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(content: Text('Engine reset to clean production mode.')),
                      );
                    }
                  },
                ),
              ],
            ),
          ),

          const SizedBox(height: AppSpacing.xl),

          // Section: Storage & Maintenance
          const SectionHeader(
            title: 'STORAGE & DATA HYGIENE',
            subtitle: 'Manage encrypted local SQLite records',
          ),
          const SizedBox(height: AppSpacing.sm),

          AppCard(
            padding: EdgeInsets.zero,
            child: AppListTile(
              leading: Container(
                padding: const EdgeInsets.all(AppSpacing.sm),
                decoration: const BoxDecoration(
                  color: AppColors.blockedLight,
                  borderRadius: AppRadius.borderSm,
                ),
                child: const Icon(LucideIcons.trash, color: AppColors.blocked, size: 18),
              ),
              title: Text(
                'Wipe All Local Audit Data',
                style: AppTextStyles.labelLarge.copyWith(
                  fontWeight: FontWeight.w600,
                  color: AppColors.blocked,
                ),
              ),
              subtitle: Text(
                'Permanently clear encrypted threat SQLite records',
                style: AppTextStyles.labelSmall.copyWith(color: AppColors.textSecondary),
              ),
              trailing: const Icon(LucideIcons.chevron_right, color: AppColors.textMuted, size: 18),
              onTap: () {
                showDialog(
                  context: context,
                  builder: (ctx) => AlertDialog(
                    backgroundColor: AppColors.surface,
                    shape: const RoundedRectangleBorder(
                      borderRadius: AppRadius.card,
                      side: BorderSide(color: AppColors.border),
                    ),
                    title: Text(
                      'Wipe All Data?',
                      style: AppTextStyles.headingMedium,
                    ),
                    content: Text(
                      'All local threat history and audit entries will be permanently deleted.',
                      style: AppTextStyles.bodyMedium.copyWith(color: AppColors.textSecondary),
                    ),
                    actions: [
                      TextButton(
                        child: Text(
                          'Cancel',
                          style: AppTextStyles.labelLarge.copyWith(color: AppColors.textSecondary),
                        ),
                        onPressed: () => Navigator.pop(ctx),
                      ),
                      AppButton(
                        label: 'Confirm Wipe',
                        variant: AppButtonVariant.danger,
                        onPressed: () async {
                          Navigator.pop(ctx);
                          await state.clearAllLogs();
                          if (context.mounted) {
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(content: Text('All local data wiped.')),
                            );
                          }
                        },
                      ),
                    ],
                  ),
                );
              },
            ),
          ),

          const SizedBox(height: AppSpacing.xl),

          // Section: About
          const SectionHeader(
            title: 'ABOUT POCKET SPARROW',
            subtitle: 'Application information and architecture',
          ),
          const SizedBox(height: AppSpacing.sm),

          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(AppSpacing.md),
                      decoration: const BoxDecoration(
                        color: AppColors.primaryLight,
                        borderRadius: AppRadius.borderMd,
                      ),
                      child: const Icon(LucideIcons.shield_check, color: AppColors.primary, size: 24),
                    ),
                    const SizedBox(width: AppSpacing.md),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Pocket Sparrow',
                            style: AppTextStyles.headingMedium,
                          ),
                          const SizedBox(height: AppSpacing.xs / 2),
                          Text(
                            'Version 1.0.0 (Hackathon Edition)',
                            style: AppTextStyles.labelSmall.copyWith(color: AppColors.textSecondary),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: AppSpacing.md),
                Text(
                  'Pocket Sparrow is a decentralized, zero-cloud security shield designed for Android and Desktop. Built to defend users against modern deceptive tactics including Unicode homoglyphs, algorithmic domains, fake bank SMS phishing, and malicious QR codes without transmitting a single byte over the wire.',
                  style: AppTextStyles.bodyMedium.copyWith(
                    color: AppColors.textSecondary,
                    height: 1.5,
                  ),
                ),
                const AppDivider(verticalMargin: AppSpacing.md),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'Local Architecture',
                      style: AppTextStyles.labelSmall.copyWith(color: AppColors.textMuted),
                    ),
                    Text(
                      'Tier 1 Heuristics (${state.isMlAvailable ? "Tier 2 ML Active" : "Sub-5ms Engine"})',
                      style: AppTextStyles.labelSmall.copyWith(
                        color: AppColors.primary,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),

          const SizedBox(height: AppSpacing.xxl),
        ],
      ),
    );
  }
}
