import 'package:flutter/material.dart';
import '../theme/app_colors.dart';
import '../theme/app_radius.dart';
import '../theme/app_spacing.dart';
import '../theme/app_text_styles.dart';

enum AppButtonVariant { primary, secondary, outline, danger }

class AppButton extends StatefulWidget {
  final String label;
  final VoidCallback? onPressed;
  final dynamic icon;
  final AppButtonVariant variant;
  final bool fullWidth;
  final bool isLoading;
  final double height;
  final EdgeInsetsGeometry? padding;

  const AppButton({
    super.key,
    required this.label,
    this.onPressed,
    this.icon,
    this.variant = AppButtonVariant.primary,
    bool fullWidth = true,
    bool? isFullWidth,
    this.isLoading = false,
    this.height = 48.0,
    this.padding,
  }) : fullWidth = isFullWidth ?? fullWidth;

  const AppButton.secondary({
    super.key,
    required this.label,
    this.onPressed,
    this.icon,
    bool fullWidth = true,
    bool? isFullWidth,
    this.isLoading = false,
    this.height = 48.0,
    this.padding,
  })  : variant = AppButtonVariant.secondary,
        fullWidth = isFullWidth ?? fullWidth;

  @override
  State<AppButton> createState() => _AppButtonState();
}

class _AppButtonState extends State<AppButton> {
  bool _isPressed = false;

  @override
  Widget build(BuildContext context) {
    final isEnabled = widget.onPressed != null && !widget.isLoading;

    Color bgColor;
    Color textColor;
    Border? border;

    switch (widget.variant) {
      case AppButtonVariant.primary:
        if (!isEnabled) {
          bgColor = AppColors.surfaceAlt;
          textColor = AppColors.textMuted;
          border = Border.all(color: AppColors.border, width: 1);
        } else if (_isPressed) {
          bgColor = AppColors.primaryDark;
          textColor = Colors.white;
        } else {
          bgColor = AppColors.primary;
          textColor = Colors.white;
        }
        break;

      case AppButtonVariant.danger:
        if (!isEnabled) {
          bgColor = AppColors.surfaceAlt;
          textColor = AppColors.textMuted;
          border = Border.all(color: AppColors.border, width: 1);
        } else if (_isPressed) {
          bgColor = const Color(0xFFB91C1C);
          textColor = Colors.white;
        } else {
          bgColor = AppColors.blocked;
          textColor = Colors.white;
        }
        break;

      case AppButtonVariant.outline:
      case AppButtonVariant.secondary:
        if (!isEnabled) {
          bgColor = Colors.transparent;
          textColor = AppColors.textMuted;
          border = Border.all(color: AppColors.border, width: 1);
        } else if (_isPressed) {
          bgColor = AppColors.surfaceAlt;
          textColor = AppColors.primaryDark;
          border = Border.all(color: AppColors.primary, width: 1);
        } else {
          bgColor = Colors.transparent;
          textColor = AppColors.textPrimary;
          border = Border.all(color: AppColors.border, width: 1);
        }
        break;
    }

    Widget? iconWidget;
    if (widget.icon is IconData) {
      iconWidget = Icon(widget.icon as IconData, size: 18, color: textColor);
    } else if (widget.icon is Widget) {
      iconWidget = widget.icon as Widget;
    }

    Widget content = widget.isLoading
        ? SizedBox(
            width: 20,
            height: 20,
            child: CircularProgressIndicator(
              strokeWidth: 2,
              valueColor: AlwaysStoppedAnimation<Color>(textColor),
            ),
          )
        : Row(
            mainAxisSize: widget.fullWidth ? MainAxisSize.max : MainAxisSize.min,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (iconWidget != null) ...[
                iconWidget,
                const SizedBox(width: AppSpacing.sm),
              ],
              Flexible(
                child: Text(
                  widget.label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: AppTextStyles.labelLarge.copyWith(
                    fontWeight: FontWeight.w600,
                    color: textColor,
                  ),
                ),
              ),
            ],
          );

    return SizedBox(
      height: widget.height,
      width: widget.fullWidth ? double.infinity : null,
      child: GestureDetector(
        onTapDown: isEnabled ? (_) => setState(() => _isPressed = true) : null,
        onTapUp: isEnabled ? (_) => setState(() => _isPressed = false) : null,
        onTapCancel: isEnabled ? () => setState(() => _isPressed = false) : null,
        onTap: isEnabled ? widget.onPressed : null,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 150),
          curve: Curves.easeOut,
          alignment: Alignment.center,
          padding: widget.padding ?? const EdgeInsets.symmetric(horizontal: AppSpacing.md),
          decoration: BoxDecoration(
            color: bgColor,
            borderRadius: AppRadius.button,
            border: border,
          ),
          child: content,
        ),
      ),
    );
  }
}
