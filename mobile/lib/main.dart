import 'package:flutter/material.dart';
import 'package:flutter_lucide/flutter_lucide.dart';
import 'package:provider/provider.dart';
import 'core/app_state_provider.dart';
import 'ui/theme/app_theme.dart';
import 'features/dashboard/dashboard_screen.dart';
import 'features/scan_url/scan_url_screen.dart';
import 'features/scan_sms/scan_sms_screen.dart';
import 'features/scan_qr/scan_qr_screen.dart';
import 'features/clipboard_monitor/clipboard_screen.dart';
import 'features/threat_log/threat_log_screen.dart';
import 'features/privacy_dashboard/privacy_screen.dart';
import 'features/settings/settings_screen.dart';
import 'features/splash/splash_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const PocketSparrowApp());
}

class PocketSparrowApp extends StatelessWidget {
  final bool skipSplash;

  const PocketSparrowApp({super.key, this.skipSplash = false});

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (_) => AppStateProvider(),
      child: MaterialApp(
        title: 'Pocket Sparrow',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.lightTheme,
        home: skipSplash ? const MainShellNavigation() : const SplashScreen(),
      ),
    );
  }
}

class MainShellNavigation extends StatefulWidget {
  const MainShellNavigation({super.key});

  @override
  State<MainShellNavigation> createState() => _MainShellNavigationState();
}

class _MainShellNavigationState extends State<MainShellNavigation> {
  int _currentIndex = 0;
  late final List<Widget> _screens;

  @override
  void initState() {
    super.initState();
    _screens = [
      DashboardScreen(onNavigateToTab: _navigateToTab),
      const ScanUrlScreen(),
      const ScanSmsScreen(),
      const ScanQrScreen(),
      const ClipboardScreen(),
      const ThreatLogScreen(),
      const PrivacyDashboardScreen(),
      const SettingsScreen(),
    ];
  }

  void _navigateToTab(int index) {
    setState(() {
      _currentIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {

    const inactiveIconColor = AppColors.inactiveIcon;

    return LayoutBuilder(
      builder: (context, constraints) {
        final isDesktopWide = constraints.maxWidth >= 760;

        if (isDesktopWide) {
          // Desktop Adaptive Layout with NavigationRail
          return Scaffold(
            backgroundColor: AppColors.background,
            body: Row(
              children: [
                SingleChildScrollView(
                  child: ConstrainedBox(
                    constraints: BoxConstraints(minHeight: constraints.maxHeight),
                    child: IntrinsicHeight(
                      child: NavigationRail(
                        backgroundColor: AppColors.surface,
                        selectedIndex: _currentIndex,
                        onDestinationSelected: (val) {
                          setState(() => _currentIndex = val);
                        },
                        labelType: NavigationRailLabelType.all,
                        useIndicator: true,
                        indicatorColor: AppColors.primaryLight,
                        selectedIconTheme: const IconThemeData(color: AppColors.primary, size: 20),
                        unselectedIconTheme: const IconThemeData(color: inactiveIconColor, size: 20),
                        selectedLabelTextStyle: AppTextStyles.labelSmall.copyWith(
                          color: AppColors.primary,
                          fontWeight: FontWeight.w600,
                        ),
                        unselectedLabelTextStyle: AppTextStyles.labelSmall.copyWith(
                          color: AppColors.textSecondary,
                          fontWeight: FontWeight.w500,
                        ),
                        leading: Padding(
                          padding: const EdgeInsets.symmetric(vertical: AppSpacing.md),
                          child: Container(
                            padding: const EdgeInsets.all(AppSpacing.sm),
                            decoration: BoxDecoration(
                              color: AppColors.primaryLight,
                              borderRadius: AppRadius.button,
                            ),
                            child: const Icon(LucideIcons.shield_check, color: AppColors.primary, size: 22),
                          ),
                        ),
                        destinations: const [
                          NavigationRailDestination(
                            icon: Icon(LucideIcons.layout_dashboard, color: inactiveIconColor),
                            selectedIcon: Icon(LucideIcons.layout_dashboard, color: AppColors.primary),
                            label: Text('Home'),
                          ),
                          NavigationRailDestination(
                            icon: Icon(LucideIcons.link, color: inactiveIconColor),
                            selectedIcon: Icon(LucideIcons.link, color: AppColors.primary),
                            label: Text('URL'),
                          ),
                          NavigationRailDestination(
                            icon: Icon(LucideIcons.message_square, color: inactiveIconColor),
                            selectedIcon: Icon(LucideIcons.message_square, color: AppColors.primary),
                            label: Text('SMS'),
                          ),
                          NavigationRailDestination(
                            icon: Icon(LucideIcons.qr_code, color: inactiveIconColor),
                            selectedIcon: Icon(LucideIcons.qr_code, color: AppColors.primary),
                            label: Text('QR'),
                          ),
                          NavigationRailDestination(
                            icon: Icon(LucideIcons.clipboard, color: inactiveIconColor),
                            selectedIcon: Icon(LucideIcons.clipboard, color: AppColors.primary),
                            label: Text('Clipboard'),
                          ),
                          NavigationRailDestination(
                            icon: Icon(LucideIcons.list_clock, color: inactiveIconColor),
                            selectedIcon: Icon(LucideIcons.list_clock, color: AppColors.primary),
                            label: Text('Audit Log'),
                          ),
                          NavigationRailDestination(
                            icon: Icon(LucideIcons.lock, color: inactiveIconColor),
                            selectedIcon: Icon(LucideIcons.lock, color: AppColors.primary),
                            label: Text('Privacy'),
                          ),
                          NavigationRailDestination(
                            icon: Icon(LucideIcons.settings, color: inactiveIconColor),
                            selectedIcon: Icon(LucideIcons.settings, color: AppColors.primary),
                            label: Text('Settings'),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
                const VerticalDivider(thickness: 1, width: 1, color: AppColors.border),
                Expanded(
                  child: IndexedStack(
                    index: _currentIndex,
                    children: _screens,
                  ),
                ),
              ],
            ),
          );
        }

        // Mobile Layout with BottomNavigationBar
        return Scaffold(
          backgroundColor: AppColors.background,
          body: IndexedStack(
            index: _currentIndex,
            children: _screens,
          ),
          bottomNavigationBar: Container(
            decoration: const BoxDecoration(
              color: AppColors.surface,
              border: Border(top: BorderSide(color: AppColors.border, width: 1.0)),
            ),
            child: BottomNavigationBar(
              currentIndex: _currentIndex,
              type: BottomNavigationBarType.fixed,
              backgroundColor: AppColors.surface,
              selectedItemColor: AppColors.primary,
              unselectedItemColor: inactiveIconColor,
              selectedFontSize: 11,
              unselectedFontSize: 10,
              selectedLabelStyle: AppTextStyles.labelSmall.copyWith(
                color: AppColors.primary,
                fontWeight: FontWeight.w600,
              ),
              unselectedLabelStyle: AppTextStyles.labelSmall.copyWith(
                color: AppColors.textSecondary,
                fontWeight: FontWeight.w500,
              ),
              elevation: 0,
              onTap: (index) {
                setState(() => _currentIndex = index);
              },
              items: const [
                BottomNavigationBarItem(
                  icon: Icon(LucideIcons.layout_dashboard, size: 20),
                  activeIcon: Icon(LucideIcons.layout_dashboard, size: 20),
                  label: 'Home',
                ),
                BottomNavigationBarItem(
                  icon: Icon(LucideIcons.link, size: 20),
                  activeIcon: Icon(LucideIcons.link, size: 20),
                  label: 'URL',
                ),
                BottomNavigationBarItem(
                  icon: Icon(LucideIcons.message_square, size: 20),
                  activeIcon: Icon(LucideIcons.message_square, size: 20),
                  label: 'SMS',
                ),
                BottomNavigationBarItem(
                  icon: Icon(LucideIcons.qr_code, size: 20),
                  activeIcon: Icon(LucideIcons.qr_code, size: 20),
                  label: 'QR',
                ),
                BottomNavigationBarItem(
                  icon: Icon(LucideIcons.clipboard, size: 20),
                  activeIcon: Icon(LucideIcons.clipboard, size: 20),
                  label: 'Clipboard',
                ),
                BottomNavigationBarItem(
                  icon: Icon(LucideIcons.list_clock, size: 20),
                  activeIcon: Icon(LucideIcons.list_clock, size: 20),
                  label: 'Logs',
                ),
                BottomNavigationBarItem(
                  icon: Icon(LucideIcons.lock, size: 20),
                  activeIcon: Icon(LucideIcons.lock, size: 20),
                  label: 'Privacy',
                ),
                BottomNavigationBarItem(
                  icon: Icon(LucideIcons.settings, size: 20),
                  activeIcon: Icon(LucideIcons.settings, size: 20),
                  label: 'Settings',
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
