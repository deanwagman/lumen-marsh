abstract class FavoritesRepository {
  Future<List<String>> loadFavoriteIds();

  Future<void> saveFavoriteIds(List<String> ids);
}
