import 'dart:io';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:pocket_sparrow/core/app_state_provider.dart';
import 'package:pocket_sparrow/ui/theme/app_theme.dart';
import 'package:pocket_sparrow/features/dashboard/dashboard_screen.dart';
import 'package:pocket_sparrow/features/scan_url/scan_url_screen.dart';
import 'package:pocket_sparrow/features/scan_sms/scan_sms_screen.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';
import 'package:pocket_sparrow/main.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    SharedPreferences.setMockInitialValues({});
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(const MethodChannel('plugins.flutter.io/path_provider'), (MethodCall methodCall) async => '.');
    sqfliteFfiInit();
    databaseFactory = databaseFactoryFfi;
  });

  testWidgets('Render Dashboard Screen to PNG', (tester) async {
    final boundaryKey = GlobalKey();
    tester.view.physicalSize = const Size(390 * 2.0, 844 * 2.0);
    tester.view.devicePixelRatio = 2.0;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });

    await tester.pumpWidget(
      ChangeNotifierProvider(
        create: (_) => AppStateProvider(),
        child: MaterialApp(
          debugShowCheckedModeBanner: false,
          theme: AppTheme.lightTheme,
          home: RepaintBoundary(
            key: boundaryKey,
            child: DashboardScreen(onNavigateToTab: (_) {}),
          ),
        ),
      ),
    );
    await tester.pump(const Duration(milliseconds: 100));
    await tester.pump(const Duration(milliseconds: 200));

    await tester.runAsync(() async {
      final boundary = boundaryKey.currentContext!.findRenderObject() as RenderRepaintBoundary;
      final image = await boundary.toImage(pixelRatio: 2.0);
      final byteData = await image.toByteData(format: ui.ImageByteFormat.png);
      final bytes = byteData!.buffer.asUint8List();
      final file = File('/home/gjgameryt-0890/.gemini/antigravity/brain/9fd02211-0ca9-4c5c-8ff4-30176abb91c1/screenshots/dashboard_after.png');
      await file.writeAsBytes(bytes);
    });
  });

  testWidgets('Render Scan SMS Screen to PNG', (tester) async {
    final boundaryKey = GlobalKey();
    tester.view.physicalSize = const Size(390 * 2.0, 844 * 2.0);
    tester.view.devicePixelRatio = 2.0;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });

    await tester.pumpWidget(
      ChangeNotifierProvider(
        create: (_) => AppStateProvider(),
        child: MaterialApp(
          debugShowCheckedModeBanner: false,
          theme: AppTheme.lightTheme,
          home: RepaintBoundary(
            key: boundaryKey,
            child: const ScanSmsScreen(),
          ),
        ),
      ),
    );
    await tester.pump(const Duration(milliseconds: 100));
    await tester.pump(const Duration(milliseconds: 200));

    await tester.runAsync(() async {
      final boundary = boundaryKey.currentContext!.findRenderObject() as RenderRepaintBoundary;
      final image = await boundary.toImage(pixelRatio: 2.0);
      final byteData = await image.toByteData(format: ui.ImageByteFormat.png);
      final bytes = byteData!.buffer.asUint8List();
      final file = File('/home/gjgameryt-0890/.gemini/antigravity/brain/9fd02211-0ca9-4c5c-8ff4-30176abb91c1/screenshots/scan_sms_after.png');
      await file.writeAsBytes(bytes);
    });
  });

  testWidgets('Render Desktop Shell with Navigation Rail to PNG', (tester) async {
    final boundaryKey = GlobalKey();
    tester.view.physicalSize = const Size(1024 * 2.0, 768 * 2.0);
    tester.view.devicePixelRatio = 2.0;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });

    await tester.pumpWidget(
      ChangeNotifierProvider(
        create: (_) => AppStateProvider(),
        child: MaterialApp(
          debugShowCheckedModeBanner: false,
          theme: AppTheme.lightTheme,
          home: RepaintBoundary(
            key: boundaryKey,
            child: const MainShellNavigation(),
          ),
        ),
      ),
    );
    await tester.pump(const Duration(milliseconds: 100));
    await tester.pump(const Duration(milliseconds: 200));

    await tester.runAsync(() async {
      final boundary = boundaryKey.currentContext!.findRenderObject() as RenderRepaintBoundary;
      final image = await boundary.toImage(pixelRatio: 2.0);
      final byteData = await image.toByteData(format: ui.ImageByteFormat.png);
      final bytes = byteData!.buffer.asUint8List();
      final file = File('/home/gjgameryt-0890/.gemini/antigravity/brain/9fd02211-0ca9-4c5c-8ff4-30176abb91c1/screenshots/desktop_shell_after.png');
      await file.writeAsBytes(bytes);
    });
  });

  testWidgets('Render Scan URL Screen with Threat Result to PNG', (tester) async {
    final boundaryKey = GlobalKey();
    tester.view.physicalSize = const Size(390 * 2.0, 844 * 2.0);
    tester.view.devicePixelRatio = 2.0;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });

    await tester.pumpWidget(
      ChangeNotifierProvider(
        create: (_) => AppStateProvider(),
        child: MaterialApp(
          debugShowCheckedModeBanner: false,
          theme: AppTheme.lightTheme,
          home: RepaintBoundary(
            key: boundaryKey,
            child: const ScanUrlScreen(),
          ),
        ),
      ),
    );
    await tester.pump(const Duration(milliseconds: 100));

    // Enter malicious URL in the input field and tap 'Check Link'
    await tester.enterText(find.byType(TextField), 'https://\u0430pple.com/login');
    await tester.runAsync(() async {
      await tester.tap(find.text('Check Link (<5ms)'));
      await Future.delayed(const Duration(milliseconds: 300));
    });
    await tester.pump(const Duration(milliseconds: 100));
    await tester.pump(const Duration(milliseconds: 200));

    // Verify detection verdict badge and score are present
    expect(find.text('Detection Result & Reasons'), findsOneWidget);
    expect(find.text('AI Confidence'), findsOneWidget);

    await tester.runAsync(() async {
      final boundary = boundaryKey.currentContext!.findRenderObject() as RenderRepaintBoundary;
      final image = await boundary.toImage(pixelRatio: 2.0);
      final byteData = await image.toByteData(format: ui.ImageByteFormat.png);
      final bytes = byteData!.buffer.asUint8List();
      final file = File('/home/gjgameryt-0890/.gemini/antigravity/brain/9fd02211-0ca9-4c5c-8ff4-30176abb91c1/screenshots/scan_url_result.png');
      await file.writeAsBytes(bytes);
    });
  });

  testWidgets('Render Scan SMS Screen with Threat Result to PNG', (tester) async {
    final boundaryKey = GlobalKey();
    tester.view.physicalSize = const Size(390 * 2.0, 844 * 2.0);
    tester.view.devicePixelRatio = 2.0;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });

    await tester.pumpWidget(
      ChangeNotifierProvider(
        create: (_) => AppStateProvider(),
        child: MaterialApp(
          debugShowCheckedModeBanner: false,
          theme: AppTheme.lightTheme,
          home: RepaintBoundary(
            key: boundaryKey,
            child: const ScanSmsScreen(),
          ),
        ),
      ),
    );
    await tester.pump(const Duration(milliseconds: 100));

    // Enter scam SMS and tap 'Analyze Message'
    await tester.enterText(find.byType(TextField), 'URGENT: Your SBI account will be blocked today. Verify now at http://sbi-kyc-update.xyz');
    await tester.runAsync(() async {
      await tester.tap(find.text('Analyze Message (<5ms)'));
      await Future.delayed(const Duration(milliseconds: 300));
    });
    await tester.pump(const Duration(milliseconds: 100));
    await tester.pump(const Duration(milliseconds: 200));

    // Verify detection verdict badge and score are present
    expect(find.text('Flagged Message Highlights'), findsOneWidget);
    expect(find.text('AI Confidence'), findsOneWidget);

    await tester.runAsync(() async {
      final boundary = boundaryKey.currentContext!.findRenderObject() as RenderRepaintBoundary;
      final image = await boundary.toImage(pixelRatio: 2.0);
      final byteData = await image.toByteData(format: ui.ImageByteFormat.png);
      final bytes = byteData!.buffer.asUint8List();
      final file = File('/home/gjgameryt-0890/.gemini/antigravity/brain/9fd02211-0ca9-4c5c-8ff4-30176abb91c1/screenshots/scan_sms_result.png');
      await file.writeAsBytes(bytes);
    });
  });
}
