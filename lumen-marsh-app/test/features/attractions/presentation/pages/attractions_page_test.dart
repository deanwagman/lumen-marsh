import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:mocktail/mocktail.dart';
import 'package:lumen_marsh_app/app/config/app_config.dart';
import 'package:lumen_marsh_app/features/attractions/data/attraction_repository.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/bloc/attractions_bloc.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/bloc/attractions_event.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/pages/attractions_page.dart';
import 'package:lumen_marsh_app/features/favorites/presentation/bloc/favorites_bloc.dart';
import 'package:lumen_marsh_app/features/favorites/presentation/bloc/favorites_state.dart';
import 'package:lumen_marsh_app/design_system/theme/lumen_theme.dart';

import '../../../../helpers/venue_live_test_support.dart';
import '../../../../helpers/seeded_attractions.dart';

class _MockAttractionRepository extends Mock implements AttractionRepository {}

class _MockFavoritesBloc extends Mock implements FavoritesBloc {}

void main() {
  late _MockAttractionRepository repository;
  late _MockFavoritesBloc favoritesBloc;

  setUp(() {
    repository = _MockAttractionRepository();
    favoritesBloc = _MockFavoritesBloc();
    when(() => repository.list()).thenAnswer((_) async => seededAttractions);
    stubIdleAttractionLiveStream(repository);
    when(() => favoritesBloc.state).thenReturn(const FavoritesLoaded([]));
    when(() => favoritesBloc.stream)
        .thenAnswer((_) => Stream.value(const FavoritesLoaded([])));
  });

  testWidgets('renders all three seeded attractions', (tester) async {
    await tester.pumpWidget(_buildPage(repository, favoritesBloc));
    await tester.pumpAndSettle();

    expect(find.text('Mangrove Run'), findsOneWidget);
    expect(find.text('Stormglass Station'), findsOneWidget);
    expect(find.text('Cypress Coil'), findsOneWidget);
    expect(find.text('Operating'), findsOneWidget);
    expect(find.text('Closed'), findsNWidgets(2));
    expect(find.textContaining('25 min wait'), findsOneWidget);
  });
}

Widget _buildPage(
  AttractionRepository repository,
  FavoritesBloc favoritesBloc,
) {
  return RepositoryProvider<AppConfig>.value(
    value: const AppConfig(apiBaseUrl: 'http://localhost:8080'),
    child: RepositoryProvider<AttractionRepository>.value(
      value: repository,
      child: MultiBlocProvider(
        providers: [
          BlocProvider(
            create: (_) =>
                AttractionsBloc(repository: repository)
                  ..add(const AttractionsRequested()),
          ),
          BlocProvider<FavoritesBloc>.value(value: favoritesBloc),
        ],
        child: MaterialApp.router(
          theme: LumenTheme.light(),
          routerConfig: GoRouter(
            initialLocation: '/attractions',
            routes: [
              GoRoute(
                path: '/attractions',
                builder: (context, state) => const AttractionsPage(),
                routes: [
                  GoRoute(
                    path: ':id',
                    builder: (context, state) =>
                        Text('detail ${state.pathParameters['id']}'),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    ),
  );
}
