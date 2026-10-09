import 'package:flutter_test/flutter_test.dart';
import 'package:pocket_sparrow/main.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('Pocket Sparrow App splash screen test', (WidgetTester tester) async {
    await tester.pumpWidget(const PocketSparrowApp());
    await tester.pump(const Duration(milliseconds: 100));

    expect(find.text('Pocket Sparrow'), findsWidgets);
    expect(find.text('100% On-Device Threat Detection'), findsOneWidget);

    // Pump past the 1.5s splash transition
    await tester.pump(const Duration(milliseconds: 1500));
    await tester.pump(const Duration(milliseconds: 500));
  });

  testWidgets('Pocket Sparrow App main shell navigation test', (WidgetTester tester) async {
    await tester.pumpWidget(const PocketSparrowApp(skipSplash: true));
    await tester.pump(const Duration(milliseconds: 100));
    await tester.pump(const Duration(milliseconds: 100));

    expect(find.text('Protection Active'), findsOneWidget);
    expect(find.text('AIRGAP ACTIVE'), findsOneWidget);
  });
}
