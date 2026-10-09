import 'package:flutter/material.dart';
import 'package:flutter_lucide/flutter_lucide.dart';
import '../theme/app_colors.dart';
import '../theme/app_radius.dart';
import '../theme/app_spacing.dart';
import '../theme/app_text_styles.dart';
import 'app_card.dart';

class ZeroBytesMeter extends StatefulWidget {
  final bool compact;

  const ZeroBytesMeter({super.key, this.compact = false});

  @override
  State<ZeroBytesMeter> createState() => _ZeroBytesMeterState();
}

class _ZeroBytesMeterState extends State<ZeroBytesMeter> with SingleTickerProviderStateMixin {
  late AnimationController _pulseController;
  late Animation<double> _glowAnimation;

  @override
  void initState() {
    super.initState();
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1800),
    )..repeat(reverse: true);

    _glowAnimation = Tween<double>(begin: 0.95, end: 1.05).animate(
      CurvedAnimation(parent: _pulseController, curve: Curves.easeInOut),
    );
  }

  @override
  void dispose() {
    _pulseController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (widget.compact) {
      return Container(
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm, vertical: AppSpacing.xs),
        decoration: BoxDecoration(
          color: AppColors.safeLight,
          borderRadius: AppRadius.badge,
          border: Border.all(color: AppColors.safe.withOpacity(0.3), width: 1.0),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(LucideIcons.wifi_off, color: AppColors.safe, size: 13),
            const SizedBox(width: AppSpacing.xs),
            Text(
              '0 BYTES SENT',
              style: AppTextStyles.labelSmall.copyWith(
                color: AppColors.safe,
                fontWeight: FontWeight.w700,
              ),
            ),
          ],
        ),
      );
    }

    return AppCard(
      backgroundColor: AppColors.safeLight,
      borderColor: AppColors.safe.withOpacity(0.35),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'OUTGOING NETWORK EGRESS',
                    style: AppTextStyles.labelSmall.copyWith(
                      color: AppColors.safe,
                      fontWeight: FontWeight.w700,
                      letterSpacing: 0.8,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  Row(
                    children: [
                      Text(
                        '0 BYTES SENT',
                        style: AppTextStyles.displayLarge.copyWith(
                          color: AppColors.safe,
                          fontWeight: FontWeight.w800,
                          fontSize: 26,
                        ),
                      ),
                      const SizedBox(width: AppSpacing.sm),
                      AnimatedBuilder(
                        animation: _glowAnimation,
                        builder: (context, child) {
                          return Transform.scale(
                            scale: _glowAnimation.value,
                            child: Container(
                              width: 10,
                              height: 10,
                              decoration: const BoxDecoration(
                                shape: BoxShape.circle,
                                color: AppColors.safe,
                              ),
                            ),
                          );
                        },
                      ),
                    ],
                  ),
                ],
              ),
              Container(
                padding: const EdgeInsets.all(AppSpacing.md),
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: AppColors.surface,
                  border: Border.all(color: AppColors.safe.withOpacity(0.3)),
                ),
                child: const Icon(LucideIcons.wifi_off, color: AppColors.safe, size: 24),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.md),
          Container(
            height: 38,
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
            decoration: BoxDecoration(
              color: AppColors.surface,
              borderRadius: AppRadius.button,
              border: Border.all(color: AppColors.safe.withOpacity(0.2)),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(LucideIcons.shield_check, color: AppColors.safe, size: 16),
                const SizedBox(width: AppSpacing.sm),
                Text(
                  '0.00 KB/s • Hard OS Airgap Guaranteed',
                  style: AppTextStyles.mono.copyWith(
                    color: AppColors.safe,
                    fontWeight: FontWeight.w600,
                    fontSize: 12,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
