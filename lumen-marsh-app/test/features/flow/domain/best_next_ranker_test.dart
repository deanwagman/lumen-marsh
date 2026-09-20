import 'package:flutter_test/flutter_test.dart';
import 'package:lumen_marsh_app/features/flow/domain/best_next_ranker.dart';
import 'package:lumen_marsh_app/features/flow/domain/guest_wait.dart';
import 'package:lumen_marsh_app/features/flow/domain/park_zone.dart';

import '../../../helpers/seeded_attractions.dart';
import '../../../helpers/seeded_flow.dart';

void main() {
  const ranker = BestNextRanker();

  test('excludes closed attractions from Best Next', () {
    final ranked = ranker.rank(
      waits: [closedMangroveWait, cypressWait],
      originZoneId: ParkZone.luminousWetlands.id,
      catalog: seededAttractions,
    );

    expect(ranked.map((item) => item.attractionId), ['cypress-coil']);
  });

  test('ranks a shorter nearby wait above a longer distant wait', () {
    final ranked = ranker.rank(
      waits: [mangroveWait, cypressWait, stormglassWait],
      originZoneId: ParkZone.cypressBasin.id,
      catalog: seededAttractions,
    );

    expect(ranked.first.attractionId, 'cypress-coil');
    expect(
      ranked.first.reason,
      'Cypress Coil is a good next choice. Its current wait is 15 minutes and is expected to remain stable.',
    );
  });

  test('boosts favorites and published destinations', () {
    final ranked = ranker.rank(
      waits: [mangroveWait, cypressWait, stormglassWait],
      originZoneId: ParkZone.luminousWetlands.id,
      favoriteIds: {'stormglass-station'},
      publishedGuidance: [publishedGuidance],
    );

    expect(ranked.first.attractionId, 'cypress-coil');
    expect(ranked.first.published, isTrue);
    expect(ranked.first.reason, publishedGuidance.guestMessage);
  });

  test('freezes ranking order when telemetry becomes stale', () {
    final previous = ranker.rank(
      waits: [mangroveWait, cypressWait],
      originZoneId: ParkZone.luminousWetlands.id,
    );
    final staleCypress = sampleGuestWait(
      attractionId: 'cypress-coil',
      displayName: 'Cypress Coil',
      originZoneId: ParkZone.cypressBasin.id,
      originZoneName: 'Cypress Basin',
      postedWaitMinutes: 5,
      waitOutlook: 'About 5 minutes',
      forecast30Minutes: 'About 5 minutes',
      freshness: QueueFreshness.stale,
    );

    final frozen = ranker.rank(
      waits: [mangroveWait, staleCypress],
      originZoneId: ParkZone.luminousWetlands.id,
      previousOrder: [for (final item in previous) item.attractionId],
    );

    expect(frozen.map((item) => item.attractionId).toList(), [
      for (final item in previous) item.attractionId,
    ]);
  });
}
