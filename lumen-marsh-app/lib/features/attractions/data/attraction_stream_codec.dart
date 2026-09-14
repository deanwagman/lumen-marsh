import 'dart:convert';

import '../domain/attraction_operational_update.dart';
import '../domain/attraction_stream_message.dart';
import '../domain/attraction_summary.dart';

class AttractionStreamCodec {
  const AttractionStreamCodec();

  AttractionStreamMessage decodeEvent({
    required String? event,
    required String data,
  }) {
    final trimmedEvent = event?.trim();
    if (trimmedEvent == null || trimmedEvent.isEmpty) {
      throw const FormatException('SSE event name is required');
    }
    if (data.trim().isEmpty) {
      throw const FormatException('SSE data is required');
    }

    final decoded = jsonDecode(data);
    return switch (trimmedEvent) {
      'attractions.snapshot' => AttractionsSnapshotMessage(
        _parseSnapshot(decoded),
      ),
      'attraction.updated' => AttractionUpdatedMessage(
        AttractionOperationalUpdate.fromJson(
          Map<String, dynamic>.from(decoded as Map),
        ),
      ),
      _ => throw FormatException('Unknown SSE event: $trimmedEvent'),
    };
  }

  List<AttractionSummary> _parseSnapshot(Object? decoded) {
    if (decoded is! List) {
      throw const FormatException('Snapshot payload must be a JSON array');
    }
    return [
      for (final item in decoded)
        AttractionSummary.fromJson(Map<String, dynamic>.from(item as Map)),
    ];
  }
}
