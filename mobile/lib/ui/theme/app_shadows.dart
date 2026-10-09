import 'package:flutter/material.dart';
import 'app_colors.dart';

class AppShadows {
  AppShadows._();

  static const BoxShadow cardShadow = BoxShadow(
    color: AppColors.shadow,
    blurRadius: 12,
    offset: Offset(0, 2),
    spreadRadius: 0,
  );

  static const BoxShadow softShadow = BoxShadow(
    color: AppColors.shadow,
    blurRadius: 6,
    offset: Offset(0, 1),
    spreadRadius: 0,
  );

  static const List<BoxShadow> card = [cardShadow];
  static const List<BoxShadow> soft = [softShadow];
}
