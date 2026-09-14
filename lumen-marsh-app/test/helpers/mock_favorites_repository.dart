import 'package:mocktail/mocktail.dart';
import 'package:lumen_marsh_app/features/favorites/domain/favorites_repository.dart';

class MockFavoritesRepository extends Mock implements FavoritesRepository {}

Future<void> stubEmptyFavorites(MockFavoritesRepository repository) async {
  when(() => repository.loadFavoriteIds()).thenAnswer((_) async => const []);
  when(() => repository.saveFavoriteIds(any())).thenAnswer((_) async {});
}

Future<void> stubFavorites(
  MockFavoritesRepository repository,
  List<String> ids,
) async {
  when(() => repository.loadFavoriteIds()).thenAnswer((_) async => ids);
  when(() => repository.saveFavoriteIds(any())).thenAnswer((_) async {});
}
