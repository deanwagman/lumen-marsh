import '../../domain/guest_advisory.dart';

List<GuestAdvisory> mergeAdvisoriesSnapshot({
  required List<GuestAdvisory> current,
  required List<GuestAdvisory> snapshot,
  required Map<String, int> withdrawnVersions,
}) {
  final currentById = {for (final advisory in current) advisory.id: advisory};
  final merged = <GuestAdvisory>[];
  for (final incoming in snapshot) {
    if (_isWithdrawn(incoming, withdrawnVersions)) {
      continue;
    }
    if (currentById[incoming.id] case final existing?
        when existing.version > incoming.version) {
      merged.add(existing);
    } else {
      merged.add(incoming);
    }
  }
  return sortAdvisories(merged);
}

List<GuestAdvisory>? applyAdvisoryLiveUpdate({
  required List<GuestAdvisory> current,
  required GuestAdvisory update,
  required Map<String, int> withdrawnVersions,
}) {
  if (_isWithdrawn(update, withdrawnVersions)) {
    return current;
  }

  final index = current.indexWhere((item) => item.id == update.id);
  if (index < 0) {
    return sortAdvisories([...current, update]);
  }

  final existing = current[index];
  if (update.version <= existing.version) {
    return current;
  }

  final next = [...current];
  next[index] = update;
  return sortAdvisories(next);
}

List<GuestAdvisory> applyAdvisoryLiveWithdrawn({
  required List<GuestAdvisory> current,
  required String advisoryId,
  required int version,
  required Map<String, int> withdrawnVersions,
}) {
  final previous = withdrawnVersions[advisoryId] ?? 0;
  if (version >= previous) {
    withdrawnVersions[advisoryId] = version;
  }
  return current.where((item) => item.id != advisoryId).toList();
}

bool _isWithdrawn(GuestAdvisory advisory, Map<String, int> withdrawnVersions) {
  final withdrawnAt = withdrawnVersions[advisory.id];
  return withdrawnAt != null && advisory.version <= withdrawnAt;
}

List<GuestAdvisory> sortAdvisories(List<GuestAdvisory> advisories) {
  final sorted = [...advisories];
  sorted.sort((a, b) {
    final severity = b.severity.index.compareTo(a.severity.index);
    if (severity != 0) {
      return severity;
    }
    return b.updatedAt.compareTo(a.updatedAt);
  });
  return sorted;
}

List<GuestAdvisory> advisoriesForAttraction(
  List<GuestAdvisory> advisories,
  String attractionId,
) {
  return sortAdvisories(
    advisories.where((advisory) => advisory.affects(attractionId)).toList(),
  );
}
