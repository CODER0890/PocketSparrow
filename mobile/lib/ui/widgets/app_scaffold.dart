import 'package:flutter/material.dart';
import '../theme/app_colors.dart';
import '../theme/app_text_styles.dart';

class AppScaffold extends StatelessWidget {
  final dynamic title; // String or Widget
  final Widget body;
  final List<Widget>? actions;
  final Widget? leading;
  final Widget? bottomNavigationBar;
  final Widget? floatingActionButton;
  final PreferredSizeWidget? bottom;
  final bool automaticallyImplyLeading;
  final bool showAppBar;

  const AppScaffold({
    super.key,
    this.title,
    required this.body,
    this.actions,
    this.leading,
    this.bottomNavigationBar,
    this.floatingActionButton,
    this.bottom,
    this.automaticallyImplyLeading = true,
    this.showAppBar = true,
  });

  @override
  Widget build(BuildContext context) {
    PreferredSizeWidget? appBar;
    if (showAppBar) {
      Widget? titleWidget;
      if (title is String) {
        titleWidget = Text(
          title as String,
          style: AppTextStyles.headingMedium,
        );
      } else if (title is Widget) {
        titleWidget = title as Widget;
      }

      appBar = PreferredSize(
        preferredSize: Size.fromHeight(kToolbarHeight + (bottom?.preferredSize.height ?? 0)),
        child: Container(
          decoration: const BoxDecoration(
            color: AppColors.surface,
            border: Border(
              bottom: BorderSide(color: AppColors.border, width: 1.0),
            ),
          ),
          child: AppBar(
            backgroundColor: AppColors.surface,
            elevation: 0,
            scrolledUnderElevation: 0,
            centerTitle: false,
            automaticallyImplyLeading: automaticallyImplyLeading,
            iconTheme: const IconThemeData(color: AppColors.textPrimary, size: 20),
            leading: leading,
            title: titleWidget,
            actions: actions,
            bottom: bottom,
          ),
        ),
      );
    }

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: appBar,
      body: SafeArea(child: body),
      bottomNavigationBar: bottomNavigationBar,
      floatingActionButton: floatingActionButton,
    );
  }
}
