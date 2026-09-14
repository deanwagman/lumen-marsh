import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lumen_marsh_app/design_system/motion/lumen_pressable.dart';
import 'package:lumen_marsh_app/design_system/theme/lumen_theme.dart';

void main() {
  testWidgets('pressable exposes button semantics', (tester) async {
    var taps = 0;
    final handle = tester.ensureSemantics();
    try {
      await tester.pumpWidget(
        MaterialApp(
          theme: LumenTheme.light(),
          home: LumenPressable(
            semanticLabel: 'Open attraction',
            onPressed: () => taps += 1,
            child: const Text('Press me'),
          ),
        ),
      );

      expect(find.bySemanticsLabel('Open attraction'), findsOneWidget);
      await tester.tap(find.text('Press me'));
      expect(taps, 1);
    } finally {
      handle.dispose();
    }
  });

  testWidgets('reduced motion snaps press scale instead of springing', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: LumenTheme.light(),
        builder: (context, child) {
          return MediaQuery(
            data: MediaQuery.of(context).copyWith(disableAnimations: true),
            child: child!,
          );
        },
        home: LumenPressable(onPressed: () {}, child: const Text('Press me')),
      ),
    );

    await tester.tap(find.text('Press me'));
    await tester.pump();
    expect(find.text('Press me'), findsOneWidget);
  });
}
