import 'package:flutter/material.dart';
import '../theme/app_colors.dart';
import '../theme/app_radius.dart';
import '../theme/app_shadows.dart';
import '../theme/app_spacing.dart';
import '../theme/app_text_styles.dart';
import 'pulse_shield.dart';

class ScanningRadar extends StatefulWidget {
  final String statusText;
  final double size;

  const ScanningRadar({
    super.key,
    this.statusText = 'Evaluating On-Device Heuristic Tree...',
    this.size = 120,
  });

  @override
  State<ScanningRadar> createState() => _ScanningRadarState();
}

class _ScanningRadarState extends State<ScanningRadar> with SingleTickerProviderStateMixin {
  late AnimationController _radarController;

  @override
  void initState() {
    super.initState();
    _radarController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1400),
    )..repeat();
  }

  @override
  void dispose() {
    _radarController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: AppSpacing.paddingLg,
      margin: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: AppRadius.card,
        border: Border.all(color: AppColors.primary.withOpacity(0.3), width: 1.0),
        boxShadow: AppShadows.card,
      ),
      child: Column(
        children: [
          SizedBox(
            width: widget.size,
            height: widget.size,
            child: Stack(
              alignment: Alignment.center,
              children: [
                AnimatedBuilder(
                  animation: _radarController,
                  builder: (context, child) {
                    final val = _radarController.value;
                    return Container(
                      width: widget.size * (0.6 + 0.4 * val),
                      height: widget.size * (0.6 + 0.4 * val),
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        border: Border.all(
                          color: AppColors.primary.withOpacity((1.0 - val).clamp(0.0, 1.0) * 0.5),
                          width: 1.5,
                        ),
                      ),
                    );
                  },
                ),
                const PulseShield(isActive: true, size: 54),
              ],
            ),
          ),
          const SizedBox(height: AppSpacing.md),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Container(
                width: 8,
                height: 8,
                decoration: const BoxDecoration(
                  shape: BoxShape.circle,
                  color: AppColors.primary,
                ),
              ),
              Flexible(
                child: Text(
                  widget.statusText,
                  style: AppTextStyles.labelLarge.copyWith(
                    color: AppColors.primary,
                    fontWeight: FontWeight.w600,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.xs),
          Text(
            'Analyzing in volatile memory • Target <5ms',
            style: AppTextStyles.labelSmall,
          ),
        ],
      ),
    );
  }
}
