import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:mocktail/mocktail.dart';
import 'package:lumen_marsh_app/app/config/app_config.dart';
import 'package:lumen_marsh_app/core/errors/app_failure.dart';
import 'package:lumen_marsh_app/features/attractions/data/attraction_repository.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/bloc/attractions_bloc.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/bloc/attractions_event.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/bloc/attractions_state.dart';
import 'package:lumen_marsh_app/features/favorites/presentation/bloc/favorites_bloc.dart';
import 'package:lumen_marsh_app/features/favorites/presentation/bloc/favorites_state.dart';
import 'package:lumen_marsh_app/features/today/presentation/pages/today_page.dart';
import 'package:lumen_marsh_app/design_system/theme/lumen_theme.dart';

import 'package:lumen_marsh_app/features/advisories/presentation/bloc/advisories_bloc.dart';

import '../../../../helpers/seeded_attractions.dart';
import '../../../../helpers/venue_live_test_support.dart';

class _MockAttractionRepository extends Mock implements AttractionRepository {}

class _MockAttractionsBloc extends Mock implements AttractionsBloc {}

class _MockFavoritesBloc extends Mock implements FavoritesBloc {}

class _MockAdvisoriesBloc extends MockAdvisoriesBloc {}

void main() {
  late _MockAttractionRepository repository;
  late _MockAttractionsBloc bloc;
  late _MockFavoritesBloc favoritesBloc;
  late _MockAdvisoriesBloc advisoriesBloc;

  setUpAll(() {
    registerFallbackValue(const AttractionsRefreshed());
    registerFallbackValue(const AttractionsRequested());
  });

  setUp(() {
    repository = _MockAttractionRepository();
    bloc = _MockAttractionsBloc();
    favoritesBloc = _MockFavoritesBloc();
    advisoriesBloc = _MockAdvisoriesBloc();
    stubEmptyAdvisories(advisoriesBloc);
    when(() => repository.list()).thenAnswer((_) async => seededAttractions);
    when(() => repository.getById('mangrove-run'))
        .thenAnswer((_) async => mangroveRunDetail);
    when(() => favoritesBloc.state).thenReturn(const FavoritesLoaded([]));
    when(() => favoritesBloc.stream)
        .thenAnswer((_) => Stream.value(const FavoritesLoaded([])));
  });

  Widget buildApp({Size size = const Size(400, 900)}) {
    return MediaQuery(
      data: MediaQueryData(size: size),
      child: RepositoryProvider<AppConfig>.value(
        value: const AppConfig(apiBaseUrl: 'http://localhost:8080'),
        child: MultiBlocProvider(
          providers: [
            BlocProvider<AttractionsBloc>.value(value: bloc),
            BlocProvider<FavoritesBloc>.value(value: favoritesBloc),
            BlocProvider<AdvisoriesBloc>.value(value: advisoriesBloc),
          ],
          child: MaterialApp.router(
            theme: LumenTheme.light(),
            routerConfig: GoRouter(
              initialLocation: '/today',
              routes: [
                GoRoute(
                  path: '/today',
                  builder: (context, state) => const TodayPage(),
                ),
                GoRoute(
                  path: '/attractions/:id',
                  builder: (context, state) => Scaffold(
                    body: Text('Detail ${state.pathParameters['id']}'),
                  ),
                ),
                GoRoute(
                  path: '/attractions',
                  builder: (context, state) =>
                      const Scaffold(body: Text('Catalog')),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  void stubState(AttractionsState state) {
    when(() => bloc.state).thenReturn(state);
    when(() => bloc.stream).thenAnswer((_) => Stream.value(state));
  }

  testWidgets('renders dashboard from loaded catalog on phone', (tester) async {
    tester.view.physicalSize = const Size(400, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    stubState(AttractionsLoaded(seededAttractions));

    await tester.pumpWidget(buildApp());
    await tester.pumpAndSettle();

    expect(find.text('1 of 3 attractions operating'), findsOneWidget);
    expect(find.text('25 min wait'), findsWidgets);
    expect(find.text('Best next adventure'), findsOneWidget);
    expect(find.byKey(const Key('today-recommendation')), findsOneWidget);
  });

  testWidgets('renders saved adventures when favorites are loaded', (
    tester,
  ) async {
    when(() => favoritesBloc.state)
        .thenReturn(const FavoritesLoaded(['stormglass-station']));
    when(() => favoritesBloc.stream).thenAnswer(
      (_) => Stream.value(const FavoritesLoaded(['stormglass-station'])),
    );
    stubState(AttractionsLoaded(seededAttractions));

    await tester.pumpWidget(buildApp());
    await tester.pumpAndSettle();

    expect(find.text('Saved Adventures'), findsOneWidget);
    expect(
      find.byKey(const Key('saved-adventure-stormglass-station')),
      findsOneWidget,
    );
    expect(find.text('Currently closed.'), findsWidgets);
  });

  testWidgets('renders wide summary layout', (tester) async {
    tester.view.physicalSize = const Size(900, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    stubState(AttractionsLoaded(seededAttractions));

    await tester.pumpWidget(buildApp(size: const Size(900, 900)));
    await tester.pumpAndSettle();

    expect(find.byType(GridView), findsOneWidget);
  });

  testWidgets('tap recommendation opens attraction detail', (tester) async {
    tester.view.physicalSize = const Size(400, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    stubState(AttractionsLoaded(seededAttractions));

    await tester.pumpWidget(buildApp());
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('today-recommendation')));
    await tester.pumpAndSettle();

    expect(find.text('Detail mangrove-run'), findsOneWidget);
  });

  testWidgets('refresh dispatches AttractionsRefreshed', (tester) async {
    stubState(AttractionsLoaded(seededAttractions));

    await tester.pumpWidget(buildApp());
    await tester.pumpAndSettle();

    await tester.tap(find.byTooltip('Refresh park conditions'));
    await tester.pump();

    verify(() => bloc.add(const AttractionsRefreshed())).called(1);
  });

  testWidgets('failure with previous data keeps operational summary', (
    tester,
  ) async {
    stubState(
      AttractionsFailure(const NetworkFailure(), previous: seededAttractions),
    );

    await tester.pumpWidget(buildApp());
    await tester.pumpAndSettle();

    expect(find.text('1 of 3 attractions operating'), findsOneWidget);
    expect(find.text('Best next adventure'), findsOneWidget);
    expect(find.text(const NetworkFailure().message), findsOneWidget);
  });
}
