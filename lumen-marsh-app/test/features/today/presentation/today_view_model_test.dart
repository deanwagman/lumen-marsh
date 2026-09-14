import 'package:flutter_test/flutter_test.dart';
import 'package:lumen_marsh_app/features/attractions/domain/attraction_enums.dart';
import 'package:lumen_marsh_app/features/attractions/domain/attraction_summary.dart';
import 'package:lumen_marsh_app/features/today/presentation/today_view_model.dart';

import '../../../helpers/seeded_attractions.dart';

void main() {
  test('recommends Mangrove Run when it is the only operating attraction', () {
    final viewModel = TodayViewModel.from(attractions: seededAttractions);

    expect(viewModel.operatingCount, 1);
    expect(viewModel.recommended?.id, 'mangrove-run');
    expect(viewModel.shortestWaitLabel, '25 min wait');
    expect(viewModel.parkSummary, '1 of 3 attractions operating');
  });

  test('never recommends closed attractions', () {
    final viewModel = TodayViewModel.from(
      attractions: [stormglassStation, cypressCoil],
    );

    expect(viewModel.recommended, isNull);
    expect(viewModel.shortestWaitLabel, 'No operating attractions');
  });

  test('includes weather and technical statuses in active notices', () {
    final weatherHold = AttractionSummary(
      id: 'mangrove-run',
      name: 'Mangrove Run',
      area: 'Luminous Wetlands',
      type: AttractionType.boatExpedition,
      status: AttractionStatus.weatherHold,
      capacityMode: AttractionCapacityMode.notApplicable,
      waitMinutes: null,
      statusMessage: 'Temporarily unavailable due to nearby weather.',
      updatedAt: DateTime.parse('2026-08-27T14:30:00Z'),
      version: 1,
    );
    final technicalDelay = AttractionSummary(
      id: 'cypress-coil',
      name: 'Cypress Coil',
      area: 'Cypress Basin',
      type: AttractionType.launchCoaster,
      status: AttractionStatus.technicalDelay,
      capacityMode: AttractionCapacityMode.notApplicable,
      waitMinutes: null,
      statusMessage: 'Experiencing a technical delay.',
      updatedAt: DateTime.parse('2026-08-27T14:31:00Z'),
      version: 2,
    );

    final viewModel = TodayViewModel.from(
      attractions: [weatherHold, technicalDelay, stormglassStation],
    );

    expect(viewModel.activeNotices, hasLength(2));
    expect(
      viewModel.activeNotices.map((a) => a.status),
      containsAll([
        AttractionStatus.weatherHold,
        AttractionStatus.technicalDelay,
      ]),
    );
  });

  test('uses the most recent updatedAt timestamp', () {
    final latest = AttractionSummary(
      id: 'stormglass-station',
      name: 'Stormglass Station',
      area: 'Research Quarter',
      type: AttractionType.indoorDarkRide,
      status: AttractionStatus.closed,
      capacityMode: AttractionCapacityMode.notApplicable,
      waitMinutes: null,
      statusMessage: 'Currently closed.',
      updatedAt: DateTime.parse('2026-08-27T15:00:00Z'),
      version: 0,
    );

    final viewModel = TodayViewModel.from(
      attractions: [mangroveRunSummary, latest],
    );

    expect(viewModel.lastUpdatedAt, latest.updatedAt);
  });

  test('resolves saved adventures from favorite ids and live catalog', () {
    final viewModel = TodayViewModel.from(
      attractions: seededAttractions,
      favoriteIds: ['stormglass-station', 'mangrove-run'],
    );

    expect(viewModel.savedAdventures, hasLength(2));
    expect(viewModel.savedAdventures.first.id, 'stormglass-station');
    expect(viewModel.savedAdventures.last.id, 'mangrove-run');
    expect(viewModel.savedAdventures.first.status, AttractionStatus.closed);
  });

  test('saved adventures stay empty when there are no favorites', () {
    final viewModel = TodayViewModel.from(attractions: seededAttractions);

    expect(viewModel.savedAdventures, isEmpty);
  });
}
