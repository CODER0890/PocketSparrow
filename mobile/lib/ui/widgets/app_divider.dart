import 'package:flutter/material.dart';
import '../theme/app_colors.dart';

class AppDivider extends StatelessWidget {
  final double height;
  final double thickness;
  final double verticalMargin;
  final Color? color;

  const AppDivider({
    super.key,
    this.height = 1.0,
    this.thickness = 1.0,
    this.verticalMargin = 0.0,
    this.color,
  });

  @override
  Widget build(BuildContext context) {
    if (verticalMargin > 0) {
      return Padding(
        padding: EdgeInsets.symmetric(vertical: verticalMargin),
        child: Divider(
          height: height,
          thickness: thickness,
          color: color ?? AppColors.border,
          indent: 0,
          endIndent: 0,
        ),
      );
    }
    return Divider(
      height: height,
      thickness: thickness,
      color: color ?? AppColors.border,
      indent: 0,
      endIndent: 0,
    );
  }
}
