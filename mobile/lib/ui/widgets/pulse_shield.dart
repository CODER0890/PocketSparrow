import 'package:flutter/material.dart';
import 'package:flutter_lucide/flutter_lucide.dart';
import '../theme/app_colors.dart';

class PulseShield extends StatefulWidget {
  final bool isActive;
  final double size;

  const PulseShield({
    super.key,
    required this.isActive,
    this.size = 56,
  });

  @override
  State<PulseShield> createState() => _PulseShieldState();
}

class _PulseShieldState extends State<PulseShield> with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _animation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1400),
    )..repeat(reverse: true);
    _animation = Tween<double>(begin: 0.95, end: 1.05).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeOut),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final bgColor = widget.isActive ? AppColors.safeLight : AppColors.surfaceAlt;
    final iconColor = widget.isActive ? AppColors.safe : AppColors.textMuted;
    final borderColor = widget.isActive ? AppColors.safe.withOpacity(0.3) : AppColors.border;

    return AnimatedBuilder(
      animation: _animation,
      builder: (context, child) {
        return Transform.scale(
          scale: widget.isActive ? _animation.value : 1.0,
          child: Container(
            width: widget.size,
            height: widget.size,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: bgColor,
              border: Border.all(color: borderColor, width: 1.5),
            ),
            child: Center(
              child: Icon(
                widget.isActive ? LucideIcons.shield_check : LucideIcons.shield_x,
                color: iconColor,
                size: widget.size * 0.52,
              ),
            ),
          ),
        );
      },
    );
  }
}
