import 'dart:convert';

import '../../../features/advisories/domain/guest_advisory.dart';
import '../../../features/attractions/domain/attraction_operational_update.dart';
import '../../../features/attractions/domain/attraction_summary.dart';
import '../../../features/flow/domain/guest_wait.dart';
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
      'guest.flow.updated' => _parseGuestFlowUpdated(decoded),
      'guest.flow.recommendation.published' =>
        GuestFlowRecommendationPublishedMessage(_parseGuidance(decoded)),
      'guest.flow.recommendation.withdrawn' =>
        GuestFlowRecommendationWithdrawnMessage(
          _parseGuidance(decoded).recommendationId,
        ),
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

  VenueStreamMessage _parseGuestFlowUpdated(Object? decoded) {
    final map = _asObject(decoded);
    if (map['attractions'] is List) {
      return GuestFlowOverviewMessage(GuestFlowOverview.fromJson(map));
    }
    final wait = GuestWait.fromJson(_unwrapPayload(map));
    return GuestFlowWaitUpdatedMessage(wait);
  }

  GuestGuidance _parseGuidance(Object? decoded) {
    return GuestGuidance.fromJson(_unwrapPayload(_asObject(decoded)));
  }

  Map<String, dynamic> _asObject(Object? decoded) {
    if (decoded is! Map) {
      throw const FormatException('Payload must be a JSON object');
    }
    return Map<String, dynamic>.from(decoded);
  }

  Map<String, dynamic> _unwrapPayload(Map<String, dynamic> map) {
    final payload = map['payload'];
    if (payload is Map) {
      return Map<String, dynamic>.from(payload);
    }
    return map;
  }
}
