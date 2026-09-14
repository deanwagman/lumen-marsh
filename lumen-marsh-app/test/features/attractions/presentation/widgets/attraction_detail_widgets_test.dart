import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lumen_marsh_app/design_system/theme/lumen_theme.dart';
import 'package:lumen_marsh_app/features/attractions/domain/attraction_detail.dart';
import 'package:lumen_marsh_app/features/attractions/domain/attraction_enums.dart';
import 'package:lumen_marsh_app/features/attractions/domain/attraction_experience.dart';
import 'package:lumen_marsh_app/features/attractions/domain/attraction_media.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/widgets/attraction_fact_grid.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/widgets/attraction_hero_image.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/widgets/attraction_status_banner.dart';

import '../../../../helpers/seeded_attractions.dart';

void main() {
  testWidgets('hero image falls back to the attraction icon on error', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SizedBox(
            height: 200,
            width: 400,
            child: AttractionHeroImage(
              imageUrl: 'http://localhost:9999/missing.webp',
              altText: mangroveRunExperience.media.altText,
              fallbackIcon: Icons.directions_boat_outlined,
              fallbackColor: Colors.green.shade900,
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.directions_boat_outlined), findsOneWidget);
  });

  testWidgets('fact grid renders experience facts and omits placeholders', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(body: AttractionFactGrid(detail: mangroveRunDetail)),
      ),
    );

    expect(find.text('8 minutes'), findsOneWidget);
    expect(find.text('Gentle'), findsOneWidget);
    expect(find.text('Outdoor'), findsOneWidget);
    expect(
      find.text('Guests must transfer into the ride vehicle.'),
      findsOneWidget,
    );
    expect(find.text('No minimum height'), findsOneWidget);
    expect(find.text('Not listed'), findsNothing);
  });

  testWidgets('closed attraction detail shows status banner', (tester) async {
    final closedDetail = AttractionDetail(
      summary: stormglassStation,
      experience: AttractionExperience(
        shortDescription: 'Explore a research outpost where stormglass instruments reveal hidden weather patterns.',
        durationMinutes: 12,
        minimumHeightInches: 42,
        intensity: Intensity.moderate,
        environment: Environment.indoor,
        singleRiderAvailable: false,
        accessibilitySummary: 'Wheelchair accessible queue and load area. Transfer required for ride vehicle.',
        media: const AttractionMedia(
          heroUrl: '/media/attractions/stormglass-station/hero.webp',
          thumbnailUrl: '/media/attractions/stormglass-station/thumbnail.webp',
          altText: 'A glass-walled research station glowing with stormglass instruments.',
        ),
      ),
    );

    await tester.pumpWidget(
      MaterialApp(
        theme: LumenTheme.light(),
        home: Scaffold(
          body: SingleChildScrollView(
            child: Column(
              children: [
                AttractionStatusBanner(attraction: closedDetail.summary),
                AttractionFactGrid(detail: closedDetail),
              ],
            ),
          ),
        ),
      ),
    );

    expect(find.text('Closed'), findsWidgets);
    expect(find.textContaining('Currently closed'), findsOneWidget);
    expect(find.text('Not listed'), findsNothing);
  });

  testWidgets('fact grid supports large text without overflow errors', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: MediaQuery(
          data: const MediaQueryData(textScaler: TextScaler.linear(2)),
          child: Scaffold(
            body: SingleChildScrollView(
              child: AttractionFactGrid(detail: mangroveRunDetail),
            ),
          ),
        ),
      ),
    );

    expect(tester.takeException(), isNull);
    expect(find.text('8 minutes'), findsOneWidget);
  });
}
