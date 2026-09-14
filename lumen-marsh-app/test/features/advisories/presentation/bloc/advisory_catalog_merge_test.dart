import 'package:flutter_test/flutter_test.dart';
import 'package:lumen_marsh_app/features/advisories/domain/advisory_severity.dart';
import 'package:lumen_marsh_app/features/advisories/domain/guest_advisory.dart';
import 'package:lumen_marsh_app/features/advisories/presentation/bloc/advisory_catalog_merge.dart';

GuestAdvisory advisory({
  required String id,
  required int version,
  AdvisorySeverity severity = AdvisorySeverity.major,
}) {
  return GuestAdvisory(
    id: id,
    severity: severity,
    title: 'Advisory $id',
    message: 'Message $version',
    affectedAttractionIds: const ['mangrove-run'],
    updatedAt: DateTime.parse('2026-09-01T15:30:00Z'),
    version: version,
  );
}

void main() {
  test('sortAdvisories orders by severity then recency', () {
    final sorted = sortAdvisories([
      advisory(id: 'minor', version: 2, severity: AdvisorySeverity.minor),
      advisory(id: 'critical', version: 1, severity: AdvisorySeverity.critical),
      advisory(id: 'major', version: 3, severity: AdvisorySeverity.major),
    ]);

    expect(sorted.map((item) => item.id).toList(), [
      'critical',
      'major',
      'minor',
    ]);
  });

  test('applyAdvisoryLiveUpdate ignores stale versions', () {
    final withdrawn = <String, int>{};
    final current = [advisory(id: 'weather-1', version: 5)];

    final next = applyAdvisoryLiveUpdate(
      current: current,
      update: advisory(id: 'weather-1', version: 4),
      withdrawnVersions: withdrawn,
    );

    expect(next, current);
    expect(identical(next, current), isTrue);
  });

  test('applyAdvisoryLiveWithdrawn prevents older republication', () {
    final withdrawn = <String, int>{};
    final current = [advisory(id: 'weather-1', version: 4)];

    final afterWithdraw = applyAdvisoryLiveWithdrawn(
      current: current,
      advisoryId: 'weather-1',
      version: 6,
      withdrawnVersions: withdrawn,
    );
    expect(afterWithdraw, isEmpty);
    expect(withdrawn['weather-1'], 6);

    final readded = applyAdvisoryLiveUpdate(
      current: afterWithdraw,
      update: advisory(id: 'weather-1', version: 5),
      withdrawnVersions: withdrawn,
    );
    expect(readded, afterWithdraw);
    expect(identical(readded, afterWithdraw), isTrue);
  });
}
