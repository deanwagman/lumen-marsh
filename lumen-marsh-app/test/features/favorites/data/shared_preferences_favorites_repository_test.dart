import 'package:flutter_test/flutter_test.dart';
import 'package:lumen_marsh_app/features/favorites/data/shared_preferences_favorites_repository.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('loadFavoriteIds returns empty list when storage is missing', () async {
    SharedPreferences.setMockInitialValues({});
    final preferences = await SharedPreferences.getInstance();
    final repository = SharedPreferencesFavoritesRepository(
      preferences: preferences,
    );

    expect(await repository.loadFavoriteIds(), isEmpty);
  });

  test('loadFavoriteIds returns empty list for invalid json', () async {
    SharedPreferences.setMockInitialValues({
      SharedPreferencesFavoritesRepository.storageKey: '{not-json',
    });
    final preferences = await SharedPreferences.getInstance();
    final repository = SharedPreferencesFavoritesRepository(
      preferences: preferences,
    );

    expect(await repository.loadFavoriteIds(), isEmpty);
  });

  test('save and load round-trip favorite ids in order', () async {
    SharedPreferences.setMockInitialValues({});
    final preferences = await SharedPreferences.getInstance();
    final repository = SharedPreferencesFavoritesRepository(
      preferences: preferences,
    );

    await repository.saveFavoriteIds(['mangrove-run', 'stormglass-station']);

    expect(await repository.loadFavoriteIds(), [
      'mangrove-run',
      'stormglass-station',
    ]);
  });
}
