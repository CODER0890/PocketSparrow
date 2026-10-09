import 'package:flutter/material.dart';
import 'package:flutter_lucide/flutter_lucide.dart';
import '../../core/threat_model.dart';
import '../theme/app_colors.dart';
import '../theme/app_radius.dart';
import '../theme/app_spacing.dart';
import '../theme/app_text_styles.dart';

class VerdictBadge extends StatefulWidget {
  final ThreatVerdict verdict;
  final bool isLarge;
  final bool animate;

  const VerdictBadge({
    super.key,
    required this.verdict,
    this.isLarge = false,
    this.animate = true,
  });

  @override
  State<VerdictBadge> createState() => _VerdictBadgeState();
}

class _VerdictBadgeState extends State<VerdictBadge> with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _scaleAnimation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 200),
    );
    _scaleAnimation = Tween<double>(begin: 0.9, end: 1.0).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeOut),
    );

    if (widget.animate) {
      _controller.forward();
    } else {
      _controller.value = 1.0;
    }
  }

  @override
  void didUpdateWidget(covariant VerdictBadge oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.verdict != widget.verdict && widget.animate) {
      _controller.forward(from: 0.0);
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    Color bgColor;
    Color textColor;
    IconData icon;
    String label;

    switch (widget.verdict) {
      case ThreatVerdict.safe:
        bgColor = AppColors.safeLight;
        textColor = AppColors.safe;
        icon = LucideIcons.shield_check;
        label = 'SAFE';
        break;
      case ThreatVerdict.caution:
        bgColor = AppColors.cautionLight;
        textColor = AppColors.caution;
        icon = LucideIcons.triangle_alert;
        label = 'CAUTION';
        break;
      case ThreatVerdict.blocked:
        bgColor = AppColors.blockedLight;
        textColor = AppColors.blocked;
        icon = LucideIcons.shield_x;
        label = 'BLOCKED';
        break;
    }

    final badge = Container(
      padding: EdgeInsets.symmetric(
        horizontal: widget.isLarge ? AppSpacing.md : AppSpacing.sm + 2,
        vertical: widget.isLarge ? AppSpacing.sm : AppSpacing.xs,
      ),
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: AppRadius.badge,
        border: Border.all(color: textColor.withOpacity(0.25), width: 1.0),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            icon,
            color: textColor,
            size: widget.isLarge ? 16 : 13,
          ),
          const SizedBox(width: AppSpacing.xs + 2),
          Text(
            label,
            style: AppTextStyles.labelSmall.copyWith(
              color: textColor,
              fontWeight: FontWeight.w700,
              letterSpacing: 0.6,
              fontSize: widget.isLarge ? 13 : 11,
            ),
          ),
        ],
      ),
    );

    if (!widget.animate) return badge;

    return ScaleTransition(
      scale: _scaleAnimation,
      child: badge,
    );
  }
}
