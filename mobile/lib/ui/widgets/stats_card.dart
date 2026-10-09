import 'package:flutter/material.dart';
import 'stat_tile.dart';

class StatsCard extends StatelessWidget {
  final String title;
  final String value;
  final IconData icon;
  final Color? iconColor;
  final String? subtitle;
  final bool animateValue;

  const StatsCard({
    super.key,
    required this.title,
    required this.value,
    required this.icon,
    this.iconColor,
    this.subtitle,
    this.animateValue = true,
  });

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: StatTile(
        label: title,
        value: value,
        icon: icon,
        helperText: subtitle,
      ),
    );
  }
}

