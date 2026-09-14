import 'package:bloc_test/bloc_test.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:lumen_marsh_app/features/favorites/presentation/bloc/favorites_bloc.dart';
import 'package:lumen_marsh_app/features/favorites/presentation/bloc/favorites_event.dart';
import 'package:lumen_marsh_app/features/favorites/presentation/bloc/favorites_state.dart';

import '../../../../helpers/mock_favorites_repository.dart';

void main() {
  late MockFavoritesRepository repository;

  setUp(() {
    repository = MockFavoritesRepository();
    registerFallbackValue(const <String>[]);
  });

  blocTest<FavoritesBloc, FavoritesState>(
    'hydrates favorites from local storage on startup',
    build: () {
      when(() => repository.loadFavoriteIds())
          .thenAnswer((_) async => ['mangrove-run']);
      return FavoritesBloc(repository: repository);
    },
    act: (bloc) => bloc.add(const FavoritesHydrated()),
    expect: () => [
      const FavoritesLoaded(['mangrove-run']),
    ],
    verify: (_) {
      verify(() => repository.loadFavoriteIds()).called(1);
    },
  );

  blocTest<FavoritesBloc, FavoritesState>(
    'missing storage hydrates to an empty set',
    build: () {
      when(() => repository.loadFavoriteIds())
          .thenAnswer((_) async => const []);
      return FavoritesBloc(repository: repository);
    },
    act: (bloc) => bloc.add(const FavoritesHydrated()),
    expect: () => [const FavoritesLoaded([])],
  );

  blocTest<FavoritesBloc, FavoritesState>(
    'toggle adds a favorite and persists it',
    build: () {
      when(() => repository.loadFavoriteIds())
          .thenAnswer((_) async => const []);
      when(() => repository.saveFavoriteIds(['mangrove-run']))
          .thenAnswer((_) async {});
      return FavoritesBloc(repository: repository);
    },
    seed: () => const FavoritesLoaded([]),
    act: (bloc) => bloc.add(const FavoriteToggled('mangrove-run')),
    expect: () => [
      const FavoritesLoaded(['mangrove-run']),
    ],
    verify: (_) {
      verify(() => repository.saveFavoriteIds(['mangrove-run'])).called(1);
    },
  );

  blocTest<FavoritesBloc, FavoritesState>(
    'toggle removes an existing favorite',
    build: () {
      when(() => repository.saveFavoriteIds([])).thenAnswer((_) async {});
      return FavoritesBloc(repository: repository);
    },
    seed: () => const FavoritesLoaded(['mangrove-run']),
    act: (bloc) => bloc.add(const FavoriteToggled('mangrove-run')),
    expect: () => [const FavoritesLoaded([])],
    verify: (_) {
      verify(() => repository.saveFavoriteIds([])).called(1);
    },
  );
}
