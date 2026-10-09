import 'package:flutter/material.dart';
import '../../core/threat_model.dart';
import '../theme/app_colors.dart';
import '../theme/app_radius.dart';
import '../theme/app_spacing.dart';
import '../theme/app_text_styles.dart';

class AppChip extends StatelessWidget {
  final String label;
  final ThreatVerdict? verdict;
  final Widget? leading;
  final bool isSelected;
  final VoidCallback? onTap;

  const AppChip({
    super.key,
    required this.label,
    this.verdict,
    this.leading,
    this.isSelected = false,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    Color bg;
    Color fg;
    Border? border;

    if (verdict != null) {
      switch (verdict!) {
        case ThreatVerdict.safe:
          bg = AppColors.safeLight;
          fg = AppColors.safe;
          break;
        case ThreatVerdict.caution:
          bg = AppColors.cautionLight;
          fg = AppColors.caution;
          break;
        case ThreatVerdict.blocked:
          bg = AppColors.blockedLight;
          fg = AppColors.blocked;
          break;
      }
    } else if (isSelected) {
      bg = AppColors.primaryLight;
      fg = AppColors.primary;
      border = Border.all(color: AppColors.primary.withOpacity(0.3), width: 1);
    } else {
      bg = AppColors.surfaceAlt;
      fg = AppColors.textPrimary;
      border = Border.all(color: AppColors.border, width: 1);
    }

    final chipWidget = Container(
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm + 2, vertical: AppSpacing.xs + 1),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: AppRadius.badge,
        border: border,
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (leading != null) ...[
            leading!,
            const SizedBox(width: AppSpacing.xs + 2),
          ],
          Flexible(
            child: Text(
              label,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: AppTextStyles.labelSmall.copyWith(
                color: fg,
                fontWeight: FontWeight.w600,
                fontSize: 11,
              ),
            ),
          ),
        ],
      ),
    );

    if (onTap != null) {
      return InkWell(
        onTap: onTap,
        borderRadius: AppRadius.badge,
        child: chipWidget,
      );
    }

    return chipWidget;
  }
}
