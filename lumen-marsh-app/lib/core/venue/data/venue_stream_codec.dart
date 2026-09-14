import 'dart:convert';

import '../../../features/advisories/domain/guest_advisory.dart';
import '../../../features/attractions/domain/attraction_operational_update.dart';
import '../../../features/attractions/domain/attraction_summary.dart';
import '../domain/venue_stream_message.dart';

class VenueStreamCodec {
  const VenueStreamCodec();

  VenueStreamMessage? decodeEvent({
    required String? event,
    required String data,
  }) {
    final trimmedEvent = event?.trim();
    if (trimmedEvent == null || trimmedEvent.isEmpty || data.trim().isEmpty) {
      return null;
    }

    final decoded = jsonDecode(data);
    return switch (trimmedEvent) {
      'attractions.snapshot' => AttractionsSnapshotMessage(
        _parseAttractionSnapshot(decoded),
      ),
      'attraction.updated' => AttractionUpdatedMessage(
        AttractionOperationalUpdate.fromJson(
          Map<String, dynamic>.from(decoded as Map),
        ),
      ),
      'advisories.snapshot' => AdvisoriesSnapshotMessage(
        _parseAdvisorySnapshot(decoded),
      ),
      'advisory.published' => AdvisoryPublishedMessage(
        GuestAdvisory.fromOperationalJson(
          Map<String, dynamic>.from(decoded as Map),
        ),
      ),
      'advisory.updated' => AdvisoryUpdatedMessage(
        GuestAdvisory.fromOperationalJson(
          Map<String, dynamic>.from(decoded as Map),
        ),
      ),
      'advisory.withdrawn' => _parseWithdrawn(decoded),
      _ => VenueStreamUnknownMessage(trimmedEvent),
    };
  }

  List<AttractionSummary> _parseAttractionSnapshot(Object? decoded) {
    if (decoded is! List) {
      throw const FormatException('Snapshot payload must be a JSON array');
    }
    return [
      for (final item in decoded)
        AttractionSummary.fromJson(Map<String, dynamic>.from(item as Map)),
    ];
  }

  List<GuestAdvisory> _parseAdvisorySnapshot(Object? decoded) {
    if (decoded is! List) {
      throw const FormatException(
        'Advisory snapshot payload must be a JSON array',
      );
    }
    return [
      for (final item in decoded)
        GuestAdvisory.fromJson(Map<String, dynamic>.from(item as Map)),
    ];
  }

  AdvisoryWithdrawnMessage _parseWithdrawn(Object? decoded) {
    if (decoded is! Map) {
      throw const FormatException('Withdrawn payload must be a JSON object');
    }
    final map = Map<String, dynamic>.from(decoded);
    final advisory = Map<String, dynamic>.from(map['advisory'] as Map);
    return AdvisoryWithdrawnMessage(
      advisory['id'] as String,
      (advisory['version'] as num).toInt(),
    );
  }
}
