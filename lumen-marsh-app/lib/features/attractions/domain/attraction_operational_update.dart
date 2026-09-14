import 'package:equatable/equatable.dart';

import 'attraction_enums.dart';
import 'attraction_live.dart';

class AttractionOperationalUpdate extends Equatable {
  const AttractionOperationalUpdate({
    required this.eventId,
    required this.eventType,
    required this.occurredAt,
    required this.attractionId,
    required this.status,
    required this.capacityMode,
    required this.waitMinutes,
    required this.statusMessage,
    required this.updatedAt,
    required this.version,
  });

  factory AttractionOperationalUpdate.fromJson(Map<String, dynamic> json) {
    final attraction = Map<String, dynamic>.from(json['attraction'] as Map);
    return AttractionOperationalUpdate(
      eventId: json['eventId'] as String,
      eventType: AttractionUpdateEventType.fromApi(json['eventType'] as String),
      occurredAt: DateTime.parse(json['occurredAt'] as String),
      attractionId: attraction['id'] as String,
      status: AttractionStatus.fromApi(attraction['status'] as String),
      capacityMode: AttractionCapacityMode.fromApi(
        attraction['capacityMode'] as String,
      ),
      waitMinutes: (attraction['waitMinutes'] as num?)?.toInt(),
      statusMessage: attraction['statusMessage'] as String?,
      updatedAt: DateTime.parse(attraction['updatedAt'] as String),
      version: (attraction['version'] as num).toInt(),
    );
  }

  final String eventId;
  final AttractionUpdateEventType eventType;
  final DateTime occurredAt;
  final String attractionId;
  final AttractionStatus status;
  final AttractionCapacityMode capacityMode;
  final int? waitMinutes;
  final String? statusMessage;
  final DateTime updatedAt;
  final int version;

  @override
  List<Object?> get props => [
    eventId,
    eventType,
    occurredAt,
    attractionId,
    status,
    capacityMode,
    waitMinutes,
    statusMessage,
    updatedAt,
    version,
  ];
}
