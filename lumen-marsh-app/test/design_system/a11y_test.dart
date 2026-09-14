import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lumen_marsh_app/design_system/components/status/lumen_notice_banner.dart';
import 'package:lumen_marsh_app/design_system/components/status/lumen_status_badge.dart';
import 'package:lumen_marsh_app/design_system/components/status/lumen_tone.dart';
import 'package:lumen_marsh_app/design_system/motion/lumen_entrance.dart';
import 'package:lumen_marsh_app/design_system/theme/lumen_theme.dart';

void main() {
  testWidgets('status badge exposes its label to semantics', (tester) async {
    final handle = tester.ensureSemantics();
    try {
      await tester.pumpWidget(
        MaterialApp(
          theme: LumenTheme.light(),
          home: const Scaffold(
            body: LumenStatusBadge(
              tone: LumenTone.positive,
              label: 'Operating',
            ),
          ),
        ),
      );

      expect(find.bySemanticsLabel('Operating'), findsOneWidget);
    } finally {
      handle.dispose();
    }
  });

  testWidgets('notice banner exposes title, message, and footer', (
    tester,
  ) async {
    final handle = tester.ensureSemantics();
    try {
      await tester.pumpWidget(
        MaterialApp(
          theme: LumenTheme.light(),
          home: const Scaffold(
            body: LumenNoticeBanner(
              tone: LumenTone.informational,
              title: 'Weather hold',
              message: 'Temporarily unavailable due to nearby weather.',
              footer: 'Seek indoor shelter until weather clears.',
            ),
          ),
        ),
      );

      expect(
        tester.getSemantics(find.byType(LumenNoticeBanner)).label,
        'Weather hold. Temporarily unavailable due to nearby weather. '
        'Seek indoor shelter until weather clears',
      );
    } finally {
      handle.dispose();
    }
  });

  testWidgets('reduced motion snaps entrance without translate', (
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
        home: const Scaffold(body: LumenEntrance(child: Text('Arrived'))),
      ),
    );

    await tester.pump();
    expect(find.text('Arrived'), findsOneWidget);
    expect(
      find.descendant(
        of: find.byType(LumenEntrance),
        matching: find.byType(Transform),
      ),
      findsNothing,
    );
  });
}
