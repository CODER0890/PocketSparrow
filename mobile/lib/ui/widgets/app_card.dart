import 'package:flutter/material.dart';
import '../theme/app_colors.dart';
import '../theme/app_radius.dart';
import '../theme/app_shadows.dart';
import '../theme/app_spacing.dart';
import '../theme/app_text_styles.dart';

class AppCard extends StatelessWidget {
  final Widget? child;
  final Widget? leading;
  final String? title;
  final String? subtitle;
  final Widget? trailing;
  final Color? backgroundColor;
  final Color? borderColor;
  final EdgeInsetsGeometry? padding;
  final EdgeInsetsGeometry? margin;
  final VoidCallback? onTap;
  final List<BoxShadow>? shadows;

  const AppCard({
    super.key,
    this.child,
    this.leading,
    this.title,
    this.subtitle,
    this.trailing,
    this.backgroundColor,
    this.borderColor,
    this.padding,
    this.margin,
    this.onTap,
    this.shadows,
  });

  @override
  Widget build(BuildContext context) {
    final effectiveBg = backgroundColor ?? AppColors.surface;
    final effectiveBorder = borderColor ?? AppColors.border;
    final effectivePadding = padding ?? AppSpacing.paddingLg;
    final effectiveShadows = shadows ?? AppShadows.card;

    Widget content;
    final hasHeader = leading != null || title != null || subtitle != null || trailing != null;

    if (hasHeader && child != null) {
      content = Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          _buildHeader(),
          const SizedBox(height: AppSpacing.md),
          child!,
        ],
      );
    } else if (hasHeader) {
      content = _buildHeader();
    } else {
      content = child ?? const SizedBox.shrink();
    }

    final cardWidget = Container(
      margin: margin,
      decoration: BoxDecoration(
        color: effectiveBg,
        borderRadius: AppRadius.card,
        border: Border.all(color: effectiveBorder, width: 1.0),
        boxShadow: effectiveShadows,
      ),
      padding: effectivePadding,
      child: content,
    );

    if (onTap != null) {
      return Material(
        color: Colors.transparent,
        borderRadius: AppRadius.card,
        child: InkWell(
          onTap: onTap,
          borderRadius: AppRadius.card,
          child: cardWidget,
        ),
      );
    }

    return cardWidget;
  }

  Widget _buildHeader() {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        if (leading != null) ...[
          leading!,
          const SizedBox(width: AppSpacing.md),
        ],
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              if (title != null)
                Text(
                  title!,
                  style: AppTextStyles.headingMedium,
                ),
              if (subtitle != null) ...[
                const SizedBox(height: AppSpacing.xs),
                Text(
                  subtitle!,
                  style: AppTextStyles.bodyMedium,
                ),
              ],
            ],
          ),
        ),
        if (trailing != null) ...[
          const SizedBox(width: AppSpacing.sm),
          trailing!,
        ],
      ],
    );
  }
}
