import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_lucide/flutter_lucide.dart';
import '../../core/threat_model.dart';
import '../theme/app_colors.dart';
import '../theme/app_radius.dart';
import '../theme/app_shadows.dart';
import '../theme/app_spacing.dart';
import '../theme/app_text_styles.dart';
import 'verdict_badge.dart';
import 'xai_reason_tile.dart';

class XaiBreakdownCard extends StatefulWidget {
  final ThreatResult result;
  final bool animateEntrance;

  const XaiBreakdownCard({
    super.key,
    required this.result,
    this.animateEntrance = true,
  });

  @override
  State<XaiBreakdownCard> createState() => _XaiBreakdownCardState();
}

class _XaiBreakdownCardState extends State<XaiBreakdownCard> with TickerProviderStateMixin {
  late AnimationController _entranceController;
  late Animation<Offset> _slideAnimation;
  late Animation<double> _fadeAnimation;

  bool _isReasonsExpanded = true;
  bool _showRawJson = false;

  @override
  void initState() {
    super.initState();

    _entranceController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 200),
    );
    _slideAnimation = Tween<Offset>(
      begin: const Offset(0, 0.1),
      end: Offset.zero,
    ).animate(CurvedAnimation(
      parent: _entranceController,
      curve: Curves.easeOut,
    ));
    _fadeAnimation = CurvedAnimation(
      parent: _entranceController,
      curve: Curves.easeOut,
    );

    if (widget.animateEntrance) {
      _entranceController.forward();
    } else {
      _entranceController.value = 1.0;
    }
  }

  @override
  void didUpdateWidget(covariant XaiBreakdownCard oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.result.rawInput != widget.result.rawInput) {
      _entranceController.forward(from: 0.0);
    }
  }

  @override
  void dispose() {
    _entranceController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final result = widget.result;
    Color accentColor;
    Color headerBg;
    switch (result.verdict) {
      case ThreatVerdict.safe:
        accentColor = AppColors.safe;
        headerBg = AppColors.safeLight;
        break;
      case ThreatVerdict.caution:
        accentColor = AppColors.caution;
        headerBg = AppColors.cautionLight;
        break;
      case ThreatVerdict.blocked:
        accentColor = AppColors.blocked;
        headerBg = AppColors.blockedLight;
        break;
    }

    final cardContent = Container(
      margin: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: AppRadius.card,
        border: Border.all(color: AppColors.border, width: 1.0),
        boxShadow: AppShadows.card,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header Banner
          Container(
            padding: AppSpacing.paddingLg,
            decoration: BoxDecoration(
              color: headerBg,
              borderRadius: const BorderRadius.vertical(top: Radius.circular(15)),
              border: Border(
                bottom: BorderSide(color: AppColors.border, width: 1.0),
              ),
            ),
            child: Wrap(
              alignment: WrapAlignment.spaceBetween,
              crossAxisAlignment: WrapCrossAlignment.center,
              spacing: AppSpacing.sm,
              runSpacing: AppSpacing.sm,
              children: [
                VerdictBadge(verdict: result.verdict, isLarge: true),
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    // Latency Badge
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm + 2, vertical: AppSpacing.xs),
                      decoration: BoxDecoration(
                        color: AppColors.surface,
                        borderRadius: AppRadius.badge,
                        border: Border.all(color: AppColors.border, width: 1.0),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(LucideIcons.zap, color: AppColors.primary, size: 13),
                          const SizedBox(width: AppSpacing.xs),
                          Text(
                            '${result.latencyMs} ms',
                            style: AppTextStyles.mono.copyWith(fontSize: 12),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: AppSpacing.sm),
                    // Tier Badge
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm + 2, vertical: AppSpacing.xs),
                      decoration: BoxDecoration(
                        color: AppColors.surface,
                        borderRadius: AppRadius.badge,
                        border: Border.all(color: AppColors.border, width: 1.0),
                      ),
                      child: Text(
                        result.tierUsed == 'ml' ? 'Tier 2 (ML)' : 'Tier 1 (Local)',
                        style: AppTextStyles.labelSmall.copyWith(
                          color: AppColors.primary,
                          fontWeight: FontWeight.w600,
                          fontSize: 11,
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),

          Padding(
            padding: AppSpacing.paddingLg,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Confidence Bar
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'AI Confidence',
                      style: AppTextStyles.labelSmall.copyWith(
                        color: AppColors.textSecondary,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    Text(
                      '${(result.confidence * 100).toInt()}%',
                      style: AppTextStyles.labelSmall.copyWith(
                        color: accentColor,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: AppSpacing.xs + 2),
                ClipRRect(
                  borderRadius: AppRadius.badge,
                  child: LinearProgressIndicator(
                    value: result.confidence,
                    backgroundColor: AppColors.surfaceAlt,
                    valueColor: AlwaysStoppedAnimation<Color>(accentColor),
                    minHeight: 6,
                  ),
                ),

                const SizedBox(height: AppSpacing.lg),

                // Accordion Heading
                InkWell(
                  onTap: () {
                    setState(() {
                      _isReasonsExpanded = !_isReasonsExpanded;
                    });
                  },
                  borderRadius: AppRadius.button,
                  child: Padding(
                    padding: const EdgeInsets.symmetric(vertical: AppSpacing.xs),
                    child: Row(
                      children: [
                        const Icon(LucideIcons.info, size: 16, color: AppColors.primary),
                        const SizedBox(width: AppSpacing.sm),
                        Expanded(
                          child: Text(
                            'Explainable AI Breakdown (${result.reasons.length})',
                            style: AppTextStyles.labelLarge.copyWith(fontWeight: FontWeight.w600),
                          ),
                        ),
                        Icon(
                          _isReasonsExpanded ? LucideIcons.chevron_up : LucideIcons.chevron_down,
                          color: AppColors.textMuted,
                          size: 16,
                        ),
                      ],
                    ),
                  ),
                ),

                if (_isReasonsExpanded) ...[
                  const SizedBox(height: AppSpacing.sm),
                  ...result.reasons.map((reason) {
                    return Padding(
                      padding: const EdgeInsets.only(bottom: AppSpacing.sm),
                      child: XaiReasonTile(
                        text: reason,
                        icon: result.isSafe
                            ? LucideIcons.shield_check
                            : (result.isCaution ? LucideIcons.triangle_alert : LucideIcons.shield_x),
                        iconColor: accentColor,
                      ),
                    );
                  }),
                ],

                if (result.flaggedTokens.isNotEmpty) ...[
                  const SizedBox(height: AppSpacing.md),
                  Text(
                    'Flagged Patterns & Indicators:',
                    style: AppTextStyles.labelSmall.copyWith(fontWeight: FontWeight.w600),
                  ),
                  const SizedBox(height: AppSpacing.sm),
                  Wrap(
                    spacing: AppSpacing.sm,
                    runSpacing: AppSpacing.sm,
                    children: result.flaggedTokens.map((token) {
                      return Container(
                        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm, vertical: AppSpacing.xs),
                        decoration: BoxDecoration(
                          color: headerBg,
                          borderRadius: AppRadius.button,
                          border: Border.all(color: accentColor.withOpacity(0.3), width: 1.0),
                        ),
                        child: Text(
                          token,
                          style: AppTextStyles.mono.copyWith(
                            color: accentColor,
                            fontWeight: FontWeight.w600,
                            fontSize: 12,
                          ),
                        ),
                      );
                    }).toList(),
                  ),
                ],

                const SizedBox(height: AppSpacing.md),
                const Divider(color: AppColors.border, height: 1.0),
                const SizedBox(height: AppSpacing.sm),

                // JSON Toggle & Copy
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Flexible(
                      child: TextButton.icon(
                        onPressed: () {
                          setState(() {
                            _showRawJson = !_showRawJson;
                          });
                        },
                        icon: Icon(
                          _showRawJson ? LucideIcons.chevron_up : LucideIcons.code,
                          size: 15,
                          color: AppColors.textSecondary,
                        ),
                        label: Text(
                          _showRawJson ? 'Hide Raw JSON' : 'Inspect Raw XAI JSON',
                          style: AppTextStyles.labelSmall.copyWith(color: AppColors.textSecondary),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ),
                    IconButton(
                      tooltip: 'Copy JSON',
                      icon: const Icon(LucideIcons.copy, size: 15, color: AppColors.textSecondary),
                      onPressed: () {
                        Clipboard.setData(
                          ClipboardData(text: const JsonEncoder.withIndent('  ').convert(result.toJson())),
                        );
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text('XAI warning JSON copied to clipboard'),
                            duration: Duration(seconds: 2),
                          ),
                        );
                      },
                    ),
                  ],
                ),

                if (_showRawJson) ...[
                  Container(
                    width: double.infinity,
                    margin: const EdgeInsets.only(top: AppSpacing.sm),
                    padding: AppSpacing.paddingMd,
                    decoration: BoxDecoration(
                      color: AppColors.surfaceAlt,
                      borderRadius: AppRadius.button,
                      border: Border.all(color: AppColors.border, width: 1.0),
                    ),
                    child: SelectableText(
                      const JsonEncoder.withIndent('  ').convert(result.toJson()),
                      style: AppTextStyles.mono.copyWith(fontSize: 11),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );

    if (!widget.animateEntrance) return cardContent;

    return SlideTransition(
      position: _slideAnimation,
      child: FadeTransition(
        opacity: _fadeAnimation,
        child: cardContent,
      ),
    );
  }
}
