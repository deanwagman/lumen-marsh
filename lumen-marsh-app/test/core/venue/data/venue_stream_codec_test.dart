import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:lumen_marsh_app/core/venue/data/venue_stream_codec.dart';
import 'package:lumen_marsh_app/core/venue/domain/venue_stream_message.dart';
import 'package:lumen_marsh_app/features/advisories/domain/advisory_severity.dart';
import 'package:lumen_marsh_app/features/advisories/domain/guest_advisory.dart';

const _advisoryRestPayload = '''
{
  "id": "weather-1",
  "severity": "MAJOR",
  "title": "Weather advisory",
  "message": "Some outdoor attractions are temporarily paused.",
  "affectedAttractionIds": ["mangrove-run", "cypress-coil"],
  "updatedAt": "2026-09-01T15:30:00Z",
  "version": 4
}
''';

const _advisoryOperationalPayload =
    '''
{
  "advisory": $_advisoryRestPayload
}
''';

GuestAdvisory sampleAdvisory({int version = 4}) {
  return GuestAdvisory(
    id: 'weather-1',
    severity: AdvisorySeverity.major,
    title: 'Weather advisory',
    message: 'Some outdoor attractions are temporarily paused.',
    affectedAttractionIds: const ['mangrove-run', 'cypress-coil'],
    updatedAt: DateTime.parse('2026-09-01T15:30:00Z'),
    version: version,
  );
}

void main() {
  const codec = VenueStreamCodec();

  group('VenueStreamCodec advisories', () {
    test('parses advisories snapshot', () {
      final message = codec.decodeEvent(
        event: 'advisories.snapshot',
        data: '[$_advisoryRestPayload]',
      );

      expect(message, isA<AdvisoriesSnapshotMessage>());
      final snapshot = message as AdvisoriesSnapshotMessage;
      expect(snapshot.advisories, hasLength(1));
      expect(snapshot.advisories.first.id, 'weather-1');
      expect(snapshot.advisories.first.severity, AdvisorySeverity.major);
    });

    test('parses advisory published and updated events', () {
      final published = codec.decodeEvent(
        event: 'advisory.published',
        data: _advisoryOperationalPayload,
      );
      final updated = codec.decodeEvent(
        event: 'advisory.updated',
        data: _advisoryOperationalPayload,
      );

      expect(published, isA<AdvisoryPublishedMessage>());
      expect(updated, isA<AdvisoryUpdatedMessage>());
      expect(
        (published as AdvisoryPublishedMessage).advisory.title,
        'Weather advisory',
      );
    });

    test('parses advisory withdrawn events', () {
      final message = codec.decodeEvent(
        event: 'advisory.withdrawn',
        data: '''
{
  "advisory": {
    "id": "weather-1",
    "version": 5
  }
}
''',
      );

      expect(message, isA<AdvisoryWithdrawnMessage>());
      final withdrawn = message as AdvisoryWithdrawnMessage;
      expect(withdrawn.advisoryId, 'weather-1');
      expect(withdrawn.version, 5);
    });

    test('returns unknown message for internal incident events', () {
      final message = codec.decodeEvent(
        event: 'incident.updated',
        data: '{"id":"internal"}',
      );

      expect(message, isA<VenueStreamUnknownMessage>());
    });

    test('rejects malformed advisory payloads', () {
      expect(
        () => codec.decodeEvent(
          event: 'advisories.snapshot',
          data: '{"not":"array"}',
        ),
        throwsA(isA<FormatException>()),
      );
      expect(
        () => codec.decodeEvent(
          event: 'advisory.published',
          data: '{"severity":"MAJOR"}',
        ),
        throwsA(anything),
      );
    });

    test('GuestAdvisory.fromJson rejects unknown severity', () {
      final json = jsonDecode(_advisoryRestPayload) as Map<String, dynamic>;
      json['severity'] = 'SEVERE';
      expect(
        () => GuestAdvisory.fromJson(json),
        throwsA(isA<FormatException>()),
      );
    });
  });
}
