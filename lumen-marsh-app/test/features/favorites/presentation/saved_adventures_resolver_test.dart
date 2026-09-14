import 'package:flutter_test/flutter_test.dart';
import 'package:lumen_marsh_app/features/favorites/presentation/saved_adventures_resolver.dart';

import '../../../helpers/seeded_attractions.dart';

void main() {
  test('preserves favorite order and joins live catalog data', () {
    final saved = resolveSavedAdventures(
      favoriteIds: ['stormglass-station', 'mangrove-run'],
      catalog: seededAttractions,
    );

    expect(saved.map((a) => a.id), ['stormglass-station', 'mangrove-run']);
    expect(saved.first.waitMinutes, isNull);
    expect(saved.last.waitMinutes, 25);
  });

  test('skips ids that are not in the current catalog', () {
    final saved = resolveSavedAdventures(
      favoriteIds: ['retired-ride', 'mangrove-run'],
      catalog: seededAttractions,
    );

    expect(saved, hasLength(1));
    expect(saved.single.id, 'mangrove-run');
  });
}
