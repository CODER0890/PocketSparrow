import 'package:flutter/material.dart';
import 'package:flutter_lucide/flutter_lucide.dart';
import '../theme/app_colors.dart';
import '../theme/app_radius.dart';
import '../theme/app_shadows.dart';
import '../theme/app_spacing.dart';
import '../theme/app_text_styles.dart';

class AppListTile extends StatelessWidget {
  final Widget? leading;
  final IconData? leadingIcon;
  final Color? leadingColor;
  final Color? leadingBgColor;
  final dynamic title;
  final dynamic subtitle;
  final Widget? trailing;
  final VoidCallback? onTap;
  final EdgeInsetsGeometry? margin;
  final EdgeInsetsGeometry? padding;

  const AppListTile({
    super.key,
    this.leading,
    this.leadingIcon,
    this.leadingColor,
    this.leadingBgColor,
    required this.title,
    this.subtitle,
    this.trailing,
    this.onTap,
    this.margin,
    this.padding,
  });

  @override
  Widget build(BuildContext context) {
    Widget? leadingWidget = leading;
    if (leadingWidget == null && leadingIcon != null) {
      leadingWidget = Container(
        width: 36,
        height: 36,
        decoration: BoxDecoration(
          color: leadingBgColor ?? AppColors.primaryLight,
          shape: BoxShape.circle,
        ),
        child: Center(
          child: Icon(
            leadingIcon,
            size: 18,
            color: leadingColor ?? AppColors.primary,
          ),
        ),
      );
    }

    Widget titleWidget;
    if (title is Widget) {
      titleWidget = title as Widget;
    } else {
      titleWidget = Text(
        (title ?? '').toString(),
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: AppTextStyles.labelLarge.copyWith(
          fontWeight: FontWeight.w600,
        ),
      );
    }

    Widget? subtitleWidget;
    if (subtitle != null) {
      if (subtitle is Widget) {
        subtitleWidget = subtitle as Widget;
      } else {
        subtitleWidget = Text(
          subtitle.toString(),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: AppTextStyles.bodyMedium.copyWith(
            color: AppColors.textSecondary,
            fontSize: 13,
          ),
        );
      }
    }

    final tileContent = Container(
      margin: margin,
      padding: padding ?? AppSpacing.paddingMd,
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: AppRadius.button,
        border: Border.all(color: AppColors.border, width: 1.0),
        boxShadow: AppShadows.soft,
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          if (leadingWidget != null) ...[
            leadingWidget,
            const SizedBox(width: AppSpacing.md),
          ],
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                titleWidget,
                if (subtitleWidget != null) ...[
                  const SizedBox(height: AppSpacing.xs),
                  subtitleWidget,
                ],
              ],
            ),
          ),
          const SizedBox(width: AppSpacing.sm),
          trailing ??
              const Icon(
                LucideIcons.chevron_right,
                size: 16,
                color: AppColors.textMuted,
              ),
        ],
      ),
    );

    if (onTap != null) {
      return Material(
        color: Colors.transparent,
        borderRadius: AppRadius.button,
        child: InkWell(
          onTap: onTap,
          borderRadius: AppRadius.button,
          child: tileContent,
        ),
      );
    }

    return tileContent;
  }
}
