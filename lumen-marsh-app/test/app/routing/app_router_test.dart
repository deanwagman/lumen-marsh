import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:lumen_marsh_app/app/app.dart';
import 'package:lumen_marsh_app/app/config/app_config.dart';
import 'package:lumen_marsh_app/app/routing/app_router.dart';
import 'package:lumen_marsh_app/core/venue/data/venue_event_hub.dart';
import 'package:lumen_marsh_app/features/advisories/data/advisory_repository.dart';
import 'package:lumen_marsh_app/features/attractions/data/attraction_repository.dart';

import '../../helpers/seeded_attractions.dart';
import '../../helpers/mock_favorites_repository.dart';
import '../../helpers/mock_field_guide_repository.dart';
import '../../helpers/venue_live_test_support.dart';

class _MockAttractionRepository extends Mock implements AttractionRepository {}

class _MockAdvisoryRepository extends Mock implements AdvisoryRepository {}

class _FakeVenueHub extends Mock implements VenueEventHub {}

void main() {
  late _MockAttractionRepository repository;
  late _MockAdvisoryRepository advisoryRepository;
  late _FakeVenueHub eventHub;
  late MockFavoritesRepository favoritesRepository;
  late MockFieldGuideRepository fieldGuideRepository;

  setUp(() {
    repository = _MockAttractionRepository();
    advisoryRepository = _MockAdvisoryRepository();
    eventHub = _FakeVenueHub();
    favoritesRepository = MockFavoritesRepository();
    fieldGuideRepository = MockFieldGuideRepository();
    when(() => repository.list()).thenAnswer((_) async => seededAttractions);
    when(() => repository.getById('mangrove-run'))
        .thenAnswer((_) async => mangroveRunDetail);
    when(() => advisoryRepository.listActive()).thenAnswer((_) async => []);
    stubIdleAttractionLiveStream(repository);
    stubIdleAdvisoryLiveStream(advisoryRepository);
    stubIdleVenueLiveStream(eventHub);
    stubEmptyFavorites(favoritesRepository);
    stubFieldGuideCatalog(fieldGuideRepository);
  });

  testWidgets('opens an attraction detail page from the catalog', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(400, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      LumenMarshApp(
        config: const AppConfig(apiBaseUrl: 'http://localhost:8080'),
        eventHub: eventHub,
        attractionRepository: repository,
        advisoryRepository: advisoryRepository,
        favoritesRepository: favoritesRepository,
        fieldGuideRepository: fieldGuideRepository,
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Mangrove Run'));
    await tester.pumpAndSettle();

    expect(find.text('25 min wait'), findsWidgets);
    expect(find.text('8 minutes'), findsOneWidget);
    expect(find.text('Gentle'), findsOneWidget);
    expect(find.text('Outdoor'), findsOneWidget);
    expect(
      find.text('Guests must transfer into the ride vehicle.'),
      findsOneWidget,
    );
    expect(find.text('No minimum height'), findsOneWidget);
    expect(find.text('Not listed'), findsNothing);
    expect(
      find.text('Glide beneath a living canopy through the luminous wetlands.'),
      findsOneWidget,
    );
    verify(() => repository.getById('mangrove-run')).called(1);
  });

  testWidgets('cold opens a field guide detail deep link', (tester) async {
    tester.view.physicalSize = const Size(400, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final router = createRouter(initialLocation: '/guide/ghost-orchid');
    await tester.pumpWidget(
      LumenMarshApp(
        config: const AppConfig(apiBaseUrl: 'http://localhost:8080'),
        eventHub: eventHub,
        attractionRepository: repository,
        advisoryRepository: advisoryRepository,
        favoritesRepository: favoritesRepository,
        fieldGuideRepository: fieldGuideRepository,
        router: router,
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Lumen Ghost Orchid'), findsOneWidget);
  });
}
