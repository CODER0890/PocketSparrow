import 'package:flutter/material.dart';
import 'package:flutter_lucide/flutter_lucide.dart';
import '../../core/threat_model.dart';
import '../theme/app_colors.dart';
import '../theme/app_radius.dart';
import '../theme/app_spacing.dart';
import '../theme/app_text_styles.dart';
import 'app_card.dart';
import 'app_list_tile.dart';
import 'verdict_badge.dart';
import 'xai_breakdown_card.dart';

class ThreatListTile extends StatelessWidget {
  final ThreatResult threat;
  final VoidCallback? onTap;

  const ThreatListTile({
    super.key,
    required this.threat,
    this.onTap,
  });

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

  String _formatTime(DateTime dt) {
    final diff = DateTime.now().difference(dt);
    if (diff.inMinutes < 1) return 'Just now';
    if (diff.inMinutes < 60) return '${diff.inMinutes}m ago';
    if (diff.inHours < 24) return '${diff.inHours}h ago';
    return '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
  }

  void _showDetailModal(BuildContext context) {
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
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg, vertical: AppSpacing.md),
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
                        child: Icon(_getTypeIcon(threat.type), color: AppColors.primary, size: 18),
                      ),
                      const SizedBox(width: AppSpacing.md),
                      Text(
                        '${threat.type.displayName} Scan Details',
                        style: AppTextStyles.headingMedium,
                      ),
                      const Spacer(),
                      VerdictBadge(verdict: threat.verdict),
                    ],
                  ),
                  const SizedBox(height: AppSpacing.md),
                  AppCard(
                    backgroundColor: AppColors.surfaceAlt,
                    padding: AppSpacing.paddingMd,
                    child: SelectableText(
                      threat.rawInput,
                      style: AppTextStyles.mono,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.md),
                  XaiBreakdownCard(result: threat),
                  const SizedBox(height: AppSpacing.lg),
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
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.sm),
      child: AppListTile(
        leadingIcon: _getTypeIcon(threat.type),
        title: threat.rawInput.trim(),
        subtitle: '${_formatTime(threat.timestamp)} • ${threat.latencyMs}ms',
        trailing: VerdictBadge(verdict: threat.verdict),
        onTap: () {
          if (onTap != null) {
            onTap!();
          } else {
            _showDetailModal(context);
          }
        },
      ),
    );
  }
}
