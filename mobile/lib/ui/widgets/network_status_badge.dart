import 'package:flutter/material.dart';
import 'package:flutter_lucide/flutter_lucide.dart';
import 'package:provider/provider.dart';
import '../../core/app_state_provider.dart';
import '../theme/app_colors.dart';
import '../theme/app_radius.dart';
import '../theme/app_spacing.dart';
import '../theme/app_text_styles.dart';

class NetworkStatusBadge extends StatelessWidget {
  final bool showIconsOnly;

  const NetworkStatusBadge({super.key, this.showIconsOnly = false});

  @override
  Widget build(BuildContext context) {
    final state = context.watch<AppStateProvider>();
    final isAirgap = state.isAirplaneModeSimulated;

    if (showIconsOnly) {
      return Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            isAirgap ? LucideIcons.wifi_off : LucideIcons.wifi,
            size: 15,
            color: isAirgap ? AppColors.safe : AppColors.textMuted,
          ),
        ],
      );
    }

    return InkWell(
      onTap: () {
        state.toggleAirplaneModeSimulated(!isAirgap);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              !isAirgap
                  ? 'Airgap Mode Active: Zero-Network Local Guarantee'
                  : 'Standard Mode: Operates 100% on-device',
            ),
            duration: const Duration(seconds: 2),
          ),
        );
      },
      borderRadius: AppRadius.badge,
      child: Container(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.sm + 2,
          vertical: AppSpacing.xs + 1,
        ),
        decoration: BoxDecoration(
          color: isAirgap ? AppColors.safeLight : AppColors.surfaceAlt,
          borderRadius: AppRadius.badge,
          border: Border.all(
            color: isAirgap ? AppColors.safe.withOpacity(0.3) : AppColors.border,
            width: 1.0,
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              isAirgap ? LucideIcons.wifi_off : LucideIcons.shield_check,
              size: 13,
              color: isAirgap ? AppColors.safe : AppColors.textMuted,
            ),
            const SizedBox(width: AppSpacing.xs + 2),
            Text(
              isAirgap ? 'AIRGAP ACTIVE' : 'ZERO-CLOUD',
              style: AppTextStyles.labelSmall.copyWith(
                fontSize: 10,
                fontWeight: FontWeight.w700,
                letterSpacing: 0.5,
                color: isAirgap ? AppColors.safe : AppColors.textSecondary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
