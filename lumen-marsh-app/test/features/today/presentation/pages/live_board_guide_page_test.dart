import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lumen_marsh_app/design_system/theme/lumen_theme.dart';
import 'package:lumen_marsh_app/features/today/presentation/pages/live_board_guide_page.dart';

void main() {
  Widget buildPage({String? sectionId, Size size = const Size(400, 900)}) {
    return MediaQuery(
      data: MediaQueryData(size: size, disableAnimations: true),
      child: MaterialApp(
        theme: LumenTheme.light(),
        home: LiveBoardGuidePage(sectionId: sectionId),
      ),
    );
  }

  testWidgets('explains waits, Best Next, and rides that still look open', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(400, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(buildPage());
    await tester.pumpAndSettle();

    expect(find.text('How the board works'), findsOneWidget);
    expect(find.byKey(const Key('live-board-guide')), findsOneWidget);
    expect(find.text('On this page'), findsOneWidget);

    expect(find.byKey(const Key('live-board-section-waits')), findsOneWidget);
    expect(
      find.byKey(const Key('live-board-section-best-next')),
      findsOneWidget,
    );
    expect(
      find.byKey(const Key('live-board-section-still-open')),
      findsOneWidget,
    );

    expect(
      find.textContaining('rounded to the nearest five minutes'),
      findsOneWidget,
    );
    expect(find.textContaining('not a reservation'), findsOneWidget);
    expect(
      find.textContaining('finer clock for the people running the ride'),
      findsOneWidget,
    );
    expect(
      find.textContaining(
        'comes off the list, even when the ride still looks open',
      ),
      findsOneWidget,
    );
    expect(
      find.textContaining(
        'does not explain anything the park has not published',
      ),
      findsOneWidget,
    );
    expect(
      find.textContaining('last picture the park published'),
      findsOneWidget,
    );
    expect(
      find.textContaining('You see the picture they publish.'),
      findsOneWidget,
    );
    expect(
      find.textContaining('The field guide stays the story of the park.'),
      findsOneWidget,
    );

    final copy = tester
        .widgetList<Text>(find.byType(Text))
        .map((text) => text.data ?? '')
        .join('\n')
        .toLowerCase();
    for (final term in [
      'work order',
      'vibration',
      'incident',
      'sensor',
      'reliability',
      'internaldescription',
      'unpublished',
      'telemetry',
    ]) {
      expect(copy.contains(term), isFalse, reason: 'guest copy names $term');
    }
  });

  testWidgets('opens on the section named in the route', (tester) async {
    tester.view.physicalSize = const Size(400, 700);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      buildPage(sectionId: 'still-open', size: const Size(400, 700)),
    );
    await tester.pumpAndSettle();

    final stillOpen = tester.getTopLeft(
      find.byKey(const Key('live-board-section-still-open')),
    );
    expect(stillOpen.dy, lessThan(120));
  });

  testWidgets('on-this-page links scroll to each section', (tester) async {
    tester.view.physicalSize = const Size(400, 700);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(buildPage(size: const Size(400, 700)));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('live-board-toc-still-open')));
    await tester.pumpAndSettle();

    final stillOpen = tester.getTopLeft(
      find.byKey(const Key('live-board-section-still-open')),
    );
    expect(stillOpen.dy, lessThan(120));
  });
}
