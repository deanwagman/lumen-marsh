import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:lumen_marsh_app/app/config/app_config.dart';
import 'package:lumen_marsh_app/features/attractions/data/attraction_repository.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/bloc/attractions_bloc.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/bloc/attractions_event.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/bloc/attractions_state.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/pages/attraction_detail_page.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/pages/attractions_page.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/bloc/attraction_detail_bloc.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/bloc/attraction_detail_event.dart';
import 'package:lumen_marsh_app/features/favorites/presentation/bloc/favorites_bloc.dart';
import 'package:lumen_marsh_app/features/favorites/presentation/bloc/favorites_event.dart';
import 'package:lumen_marsh_app/features/favorites/presentation/bloc/favorites_state.dart';
import 'package:lumen_marsh_app/features/favorites/domain/favorites_repository.dart';
import 'package:lumen_marsh_app/features/advisories/data/advisory_repository.dart';
import 'package:lumen_marsh_app/features/advisories/presentation/bloc/advisories_bloc.dart';
import 'package:lumen_marsh_app/features/advisories/presentation/bloc/advisories_event.dart';
import 'package:lumen_marsh_app/features/today/presentation/pages/today_page.dart';
import 'package:lumen_marsh_app/design_system/theme/lumen_theme.dart';

import '../../../helpers/venue_live_test_support.dart';
import '../../../helpers/seeded_attractions.dart';

class _MockAttractionRepository extends Mock implements AttractionRepository {}

class _MockAdvisoryRepository extends Mock implements AdvisoryRepository {}

class _RecordingFavoritesRepository extends Mock
    implements FavoritesRepository {}

void main() {
  late _MockAttractionRepository repository;
  late _MockAdvisoryRepository advisoryRepository;
  late _RecordingFavoritesRepository favoritesRepository;

  setUpAll(() {
    registerFallbackValue(const AttractionsRefreshed());
    registerFallbackValue(const AttractionsRequested());
    registerFallbackValue(const FavoriteToggled('mangrove-run'));
    registerFallbackValue(const AdvisoriesRequested());
    registerFallbackValue(const <String>[]);
  });

  setUp(() {
    repository = _MockAttractionRepository();
    advisoryRepository = _MockAdvisoryRepository();
    favoritesRepository = _RecordingFavoritesRepository();
    when(() => repository.list()).thenAnswer((_) async => seededAttractions);
    when(() => repository.getById('mangrove-run'))
        .thenAnswer((_) async => mangroveRunDetail);
    stubIdleAttractionLiveStream(repository);
    when(() => advisoryRepository.listActive())
        .thenAnswer((_) async => const []);
    stubIdleAdvisoryLiveStream(advisoryRepository);
    when(() => favoritesRepository.loadFavoriteIds())
        .thenAnswer((_) async => const []);
    when(() => favoritesRepository.saveFavoriteIds(any()))
        .thenAnswer((_) async {});
  });

  testWidgets('favorite toggle updates catalog and today screens', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(400, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final favoritesBloc = FavoritesBloc(repository: favoritesRepository)
      ..add(const FavoritesHydrated());
    final attractionsBloc = AttractionsBloc(repository: repository)
      ..add(const AttractionsRequested());

    await tester.pumpWidget(
      RepositoryProvider<AppConfig>.value(
        value: const AppConfig(apiBaseUrl: 'http://localhost:8080'),
        child: MultiBlocProvider(
          providers: [
            BlocProvider.value(value: attractionsBloc),
            BlocProvider.value(value: favoritesBloc),
          ],
          child: MaterialApp(
            theme: LumenTheme.light(),
            home: const AttractionsPage(),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.favorite), findsNothing);

    favoritesBloc.add(const FavoriteToggled('mangrove-run'));
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.favorite), findsOneWidget);

    final advisoriesBloc = AdvisoriesBloc(repository: advisoryRepository)
      ..add(const AdvisoriesRequested());

    await tester.pumpWidget(
      RepositoryProvider<AppConfig>.value(
        value: const AppConfig(apiBaseUrl: 'http://localhost:8080'),
        child: MultiBlocProvider(
          providers: [
            BlocProvider.value(value: attractionsBloc),
            BlocProvider.value(value: favoritesBloc),
            BlocProvider.value(value: advisoriesBloc),
          ],
          child: MaterialApp(
            theme: LumenTheme.light(),
            home: const TodayPage(),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Saved Adventures'), findsOneWidget);
    expect(
      find.byKey(const Key('saved-adventure-mangrove-run')),
      findsOneWidget,
    );
  });

  testWidgets('detail heart toggles selected state for screen readers', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(400, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final favoritesBloc = FavoritesBloc(repository: favoritesRepository)
      ..add(const FavoritesHydrated());
    await favoritesBloc.stream.firstWhere((state) => state is FavoritesLoaded);
    final attractionsBloc = AttractionsBloc(repository: repository)
      ..add(const AttractionsRequested());
    await attractionsBloc.stream.firstWhere(
      (state) => state is AttractionsLoaded,
    );
    final detailBloc = AttractionDetailBloc(repository: repository)
      ..add(const AttractionDetailRequested('mangrove-run'));
    final advisoriesBloc = AdvisoriesBloc(repository: advisoryRepository)
      ..add(const AdvisoriesRequested());

    await tester.pumpWidget(
      RepositoryProvider<AppConfig>.value(
        value: const AppConfig(apiBaseUrl: 'http://localhost:8080'),
        child: MultiBlocProvider(
          providers: [
            BlocProvider.value(value: attractionsBloc),
            BlocProvider.value(value: favoritesBloc),
            BlocProvider.value(value: advisoriesBloc),
            BlocProvider.value(value: detailBloc),
          ],
          child: MaterialApp(
            theme: LumenTheme.light(),
            home: const AttractionDetailPage(attractionId: 'mangrove-run'),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('attraction-favorite-button')), findsOneWidget);
    expect(find.byIcon(Icons.favorite_border), findsOneWidget);

    await tester.tap(find.byKey(const Key('attraction-favorite-button')));
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.favorite), findsOneWidget);
    expect(find.byTooltip('Remove from saved adventures'), findsOneWidget);
    verify(() => favoritesRepository.saveFavoriteIds(['mangrove-run']))
        .called(1);
  });
}
