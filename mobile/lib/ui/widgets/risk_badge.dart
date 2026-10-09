import 'package:flutter/material.dart';
import '../../core/threat_model.dart';
import 'verdict_badge.dart';

class RiskBadge extends StatelessWidget {
  final ThreatVerdict verdict;
  final bool isLarge;

  const RiskBadge({
    super.key,
    required this.verdict,
    this.isLarge = false,
  });

  @override
  Widget build(BuildContext context) {
    return VerdictBadge(verdict: verdict, isLarge: isLarge);
  }
}

