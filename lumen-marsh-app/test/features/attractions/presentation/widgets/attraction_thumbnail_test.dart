import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/widgets/attraction_thumbnail.dart';

void main() {
  testWidgets('thumbnail falls back to icon when image fails', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: AttractionThumbnail(
            imageUrl: 'http://localhost:9999/missing.webp',
            altText: 'Missing thumbnail',
            fallbackIcon: Icons.directions_boat_outlined,
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.directions_boat_outlined), findsOneWidget);
  });

  testWidgets('thumbnail falls back when url is missing', (tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(
          body: AttractionThumbnail(
            imageUrl: null,
            altText: null,
            fallbackIcon: Icons.directions_boat_outlined,
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.directions_boat_outlined), findsOneWidget);
  });
}
