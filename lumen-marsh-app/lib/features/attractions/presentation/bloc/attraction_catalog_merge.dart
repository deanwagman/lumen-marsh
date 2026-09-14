import '../../domain/attraction_operational_update.dart';
import '../../domain/attraction_summary.dart';

List<AttractionSummary> mergeAttractionsSnapshot({
  required List<AttractionSummary> current,
  required List<AttractionSummary> snapshot,
}) {
  final currentById = {
    for (final attraction in current) attraction.id: attraction,
  };
  return [
    for (final incoming in snapshot)
      if (currentById[incoming.id] case final existing?
          when existing.version > incoming.version)
        existing
      else
        incoming,
  ];
}

List<AttractionSummary>? applyAttractionUpdate({
  required List<AttractionSummary> current,
  required AttractionOperationalUpdate update,
}) {
  final index = current.indexWhere((item) => item.id == update.attractionId);
  if (index < 0) {
    return null;
  }

  final existing = current[index];
  if (update.version <= existing.version) {
    return current;
  }

  final next = [...current];
  next[index] = existing.applyOperationalUpdate(update);
  return next;
}
