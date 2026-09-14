import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import '../domain/favorites_repository.dart';

class SharedPreferencesFavoritesRepository implements FavoritesRepository {
  SharedPreferencesFavoritesRepository({required this.preferences});

  static const storageKey = 'saved_adventure_ids';

  final SharedPreferences preferences;

  @override
  Future<List<String>> loadFavoriteIds() async {
    final raw = preferences.getString(storageKey);
    if (raw == null || raw.isEmpty) {
      return const [];
    }

    try {
      final decoded = jsonDecode(raw);
      if (decoded is! List) {
        return const [];
      }
      return decoded.whereType<String>().toList();
    } catch (_) {
      return const [];
    }
  }

  @override
  Future<void> saveFavoriteIds(List<String> ids) async {
    await preferences.setString(storageKey, jsonEncode(ids));
  }
}
