import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:lumen_marsh_app/features/attractions/data/attraction_stream_codec.dart';
import 'package:lumen_marsh_app/features/attractions/data/sse_event_parser.dart';
import 'package:lumen_marsh_app/features/attractions/domain/attraction_enums.dart';
import 'package:lumen_marsh_app/features/attractions/domain/attraction_live.dart';
import 'package:lumen_marsh_app/features/attractions/domain/attraction_stream_message.dart';

import '../../../helpers/seeded_attractions.dart';

const _updatePayload = '''
{
  "eventId": "evt-1",
  "eventType": "WAIT_TIME_CHANGED",
  "occurredAt": "2026-08-27T15:00:00Z",
  "attraction": {
    "id": "mangrove-run",
    "status": "OPERATING",
    "capacityMode": "NORMAL",
    "waitMinutes": 40,
    "statusMessage": null,
    "updatedAt": "2026-08-27T15:00:00Z",
    "version": 3
  }
}
''';

void main() {
  const codec = AttractionStreamCodec();

  group('AttractionStreamCodec', () {
    test('parses a snapshot payload', () {
      final message = codec.decodeEvent(
        event: 'attractions.snapshot',
        data: seededAttractionsJson,
      );

      expect(message, isA<AttractionsSnapshotMessage>());
      final snapshot = message as AttractionsSnapshotMessage;
      expect(snapshot.attractions, hasLength(3));
      expect(snapshot.attractions.first.id, 'mangrove-run');
      expect(snapshot.attractions.first.waitMinutes, 25);
    });

    test('parses an update with nullable wait time', () {
      final closed = jsonDecode(_updatePayload) as Map<String, dynamic>;
      final attraction = Map<String, dynamic>.from(closed['attraction'] as Map);
      attraction['waitMinutes'] = null;
      attraction['status'] = 'CLOSED';
      closed['attraction'] = attraction;
      closed['eventType'] = 'STATUS_CHANGED';

      final message = codec.decodeEvent(
        event: 'attraction.updated',
        data: jsonEncode(closed),
      );

      expect(message, isA<AttractionUpdatedMessage>());
      final update = (message as AttractionUpdatedMessage).update;
      expect(update.attractionId, 'mangrove-run');
      expect(update.waitMinutes, isNull);
      expect(update.status, AttractionStatus.closed);
      expect(update.eventType, AttractionUpdateEventType.statusChanged);
    });

    test('parses wait-time updates', () {
      final message = codec.decodeEvent(
        event: 'attraction.updated',
        data: _updatePayload,
      );
      final update = (message as AttractionUpdatedMessage).update;
      expect(update.waitMinutes, 40);
      expect(update.version, 3);
      expect(update.eventType, AttractionUpdateEventType.waitTimeChanged);
    });

    test('rejects unknown enum values', () {
      expect(
        () => codec.decodeEvent(
          event: 'attraction.updated',
          data: '''
{
  "eventId": "evt-1",
  "eventType": "WAIT_TIME_CHANGED",
  "occurredAt": "2026-08-27T15:00:00Z",
  "attraction": {
    "id": "mangrove-run",
    "status": "ON_FIRE",
    "capacityMode": "NORMAL",
    "waitMinutes": 40,
    "statusMessage": null,
    "updatedAt": "2026-08-27T15:00:00Z",
    "version": 3
  }
}
''',
        ),
        throwsA(isA<FormatException>()),
      );
    });

    test('rejects malformed JSON', () {
      expect(
        () =>
            codec.decodeEvent(event: 'attractions.snapshot', data: '{not-json'),
        throwsA(anything),
      );
    });
  });

  group('SseEventParser', () {
    test('assembles events split across network chunks', () {
      final parser = SseEventParser();
      expect(parser.addChunk('event: attractions.snapshot\n'), isEmpty);
      expect(parser.addChunk('data: [{"id":"a"'), isEmpty);
      final events = parser.addChunk('}]\n\n');
      expect(events, hasLength(1));
      expect(events.single.event, 'attractions.snapshot');
      expect(events.single.data, '[{"id":"a"}]');
    });

    test('handles multiple events in one chunk', () {
      final parser = SseEventParser();
      final events = parser.addChunk(
        'event: attraction.updated\ndata: {"a":1}\n\n'
        'event: attractions.snapshot\ndata: []\n\n',
      );
      expect(events, hasLength(2));
      expect(events[0].event, 'attraction.updated');
      expect(events[1].event, 'attractions.snapshot');
    });
  });

  group('AttractionSummary.applyOperationalUpdate', () {
    test('updates operational fields and preserves identity media', () {
      final updated = mangroveRunSummary.applyOperationalUpdate(
        (codec.decodeEvent(
          event: 'attraction.updated',
          data: _updatePayload,
        ) as AttractionUpdatedMessage).update,
      );

      expect(updated.waitMinutes, 40);
      expect(updated.version, 3);
      expect(updated.name, mangroveRunSummary.name);
      expect(updated.area, mangroveRunSummary.area);
      expect(updated.type, mangroveRunSummary.type);
      expect(updated.thumbnailUrl, mangroveRunSummary.thumbnailUrl);
    });
  });
}
