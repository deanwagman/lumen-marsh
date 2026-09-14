import '../../attractions/domain/attraction_summary.dart';

List<AttractionSummary> resolveSavedAdventures({
  required List<String> favoriteIds,
  required List<AttractionSummary> catalog,
}) {
  if (favoriteIds.isEmpty || catalog.isEmpty) {
    return const [];
  }

  final byId = {for (final attraction in catalog) attraction.id: attraction};
  return [
    for (final id in favoriteIds)
      if (byId.containsKey(id)) byId[id]!,
  ];
}
