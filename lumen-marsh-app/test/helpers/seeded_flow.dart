import 'package:lumen_marsh_app/features/flow/domain/guest_wait.dart';
import 'package:lumen_marsh_app/features/flow/domain/park_zone.dart';

GuestWait sampleGuestWait({
  String attractionId = 'mangrove-run',
  String displayName = 'Mangrove Run',
  String originZoneId = ParkZone.luminousWetlandsId,
  String originZoneName = 'Luminous Wetlands',
  int? postedWaitMinutes = 20,
  String? waitOutlook = 'About 20 minutes',
  String? forecast30Minutes = 'About 25 minutes',
  GuestWaitTrend trend = GuestWaitTrend.likelyStable,
  GuestAvailability availability = GuestAvailability.operating,
  QueueFreshness freshness = QueueFreshness.fresh,
  String updatedAt = '2026-09-15T18:30:00Z',
  bool simulated = true,
}) {
  return GuestWait(
    attractionId: attractionId,
    displayName: displayName,
    originZoneId: originZoneId,
    originZoneName: originZoneName,
    postedWaitMinutes: postedWaitMinutes,
    waitOutlook: waitOutlook,
    forecast30Minutes: forecast30Minutes,
    trend: trend,
    availability: availability,
    freshness: freshness,
    updatedAt: DateTime.parse(updatedAt),
    simulated: simulated,
  );
}

final mangroveWait = sampleGuestWait();

final cypressWait = sampleGuestWait(
  attractionId: 'cypress-coil',
  displayName: 'Cypress Coil',
  originZoneId: ParkZone.cypressBasin.id,
  originZoneName: 'Cypress Basin',
  postedWaitMinutes: 15,
  waitOutlook: 'About 15 minutes',
  forecast30Minutes: 'About 15 minutes',
  trend: GuestWaitTrend.likelyStable,
);

final stormglassWait = sampleGuestWait(
  attractionId: 'stormglass-station',
  displayName: 'Stormglass Station',
  originZoneId: ParkZone.researchQuarter.id,
  originZoneName: 'Research Quarter',
  postedWaitMinutes: 35,
  waitOutlook: 'About 35 minutes',
  forecast30Minutes: 'About 40 minutes',
  trend: GuestWaitTrend.likelyRising,
);

final closedMangroveWait = sampleGuestWait(
  postedWaitMinutes: null,
  waitOutlook: null,
  forecast30Minutes: null,
  availability: GuestAvailability.unavailable,
  freshness: QueueFreshness.fresh,
);

final publishedGuidance = GuestGuidance(
  recommendationId: 'rec-1',
  recommendedDestinationIds: const ['cypress-coil', 'stormglass-station'],
  guestMessage: 'Mangrove Run is temporarily unavailable. Lantern Ferry currently has a shorter wait.',
  updatedAt: DateTime.parse('2026-09-15T18:31:00Z'),
  simulated: true,
);

const guestFlowOverviewJson = '''
{
  "attractions": [
    {
      "attractionId": "mangrove-run",
      "displayName": "Mangrove Run",
      "originZoneId": "luminous-wetlands",
      "originZoneName": "Luminous Wetlands",
      "postedWaitMinutes": 20,
      "waitOutlook": "About 20 minutes",
      "forecast30Minutes": "About 25 minutes",
      "trend": "LIKELY_STABLE",
      "availability": "OPERATING",
      "freshness": "FRESH",
      "updatedAt": "2026-09-15T18:30:00Z",
      "simulated": true
    },
    {
      "attractionId": "cypress-coil",
      "displayName": "Cypress Coil",
      "originZoneId": "cypress-basin",
      "originZoneName": "Cypress Basin",
      "postedWaitMinutes": 15,
      "waitOutlook": "About 15 minutes",
      "forecast30Minutes": "About 15 minutes",
      "trend": "LIKELY_STABLE",
      "availability": "OPERATING",
      "freshness": "FRESH",
      "updatedAt": "2026-09-15T18:30:00Z",
      "simulated": true
    }
  ],
  "publishedGuidance": [],
  "updatedAt": "2026-09-15T18:30:00Z",
  "simulated": true
}
''';

const guestWaitJson = '''
{
  "attractionId": "mangrove-run",
  "displayName": "Mangrove Run",
  "originZoneId": "luminous-wetlands",
  "originZoneName": "Luminous Wetlands",
  "postedWaitMinutes": 20,
  "waitOutlook": "About 20 minutes",
  "forecast30Minutes": "About 25 minutes",
  "trend": "LIKELY_STABLE",
  "availability": "OPERATING",
  "freshness": "FRESH",
  "updatedAt": "2026-09-15T18:30:00Z",
  "simulated": true
}
''';

const guestGuidanceJson = '''
{
  "recommendationId": "rec-1",
  "recommendedDestinationIds": ["cypress-coil", "stormglass-station"],
  "guestMessage": "Mangrove Run is temporarily unavailable. Choose Cypress Coil for a shorter wait.",
  "updatedAt": "2026-09-15T18:31:00Z",
  "simulated": true
}
''';
