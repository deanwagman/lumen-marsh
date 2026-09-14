import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/widgets/live_updating.dart';

void main() {
  testWidgets('LiveUpdating respects reduced motion', (tester) async {
    await tester.pumpWidget(
      const MediaQuery(
        data: MediaQueryData(disableAnimations: true),
        child: MaterialApp(
          home: Scaffold(
            body: LiveUpdating(animationKey: 'a', child: Text('first')),
          ),
        ),
      ),
    );
    expect(find.text('first'), findsOneWidget);

    await tester.pumpWidget(
      const MediaQuery(
        data: MediaQueryData(disableAnimations: true),
        child: MaterialApp(
          home: Scaffold(
            body: LiveUpdating(animationKey: 'b', child: Text('second')),
          ),
        ),
      ),
    );
    await tester.pump();
    expect(find.text('second'), findsOneWidget);
    expect(find.byType(AnimatedSwitcher), findsNothing);
  });
}
