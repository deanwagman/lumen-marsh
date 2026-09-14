import 'package:equatable/equatable.dart';

import 'attraction_enums.dart';
import 'attraction_operational_update.dart';

class AttractionSummary extends Equatable with AttractionOperationalFields {
  const AttractionSummary({
    required this.id,
    required this.name,
    required this.area,
    required this.type,
    required this.status,
    required this.capacityMode,
    required this.waitMinutes,
    required this.statusMessage,
    required this.updatedAt,
    required this.version,
    this.thumbnailUrl,
    this.thumbnailAltText,
  });

  factory AttractionSummary.fromJson(Map<String, dynamic> json) {
    return AttractionSummary(
      id: json['id'] as String,
      name: json['name'] as String,
      area: json['area'] as String,
      type: AttractionType.fromApi(json['type'] as String),
      status: AttractionStatus.fromApi(json['status'] as String),
      capacityMode: AttractionCapacityMode.fromApi(
        json['capacityMode'] as String,
      ),
      waitMinutes: (json['waitMinutes'] as num?)?.toInt(),
      statusMessage: json['statusMessage'] as String?,
      updatedAt: DateTime.parse(json['updatedAt'] as String),
      version: (json['version'] as num).toInt(),
      thumbnailUrl: json['thumbnailUrl'] as String?,
      thumbnailAltText: json['thumbnailAltText'] as String?,
    );
  }

  AttractionSummary applyOperationalUpdate(AttractionOperationalUpdate update) {
    return AttractionSummary(
      id: id,
      name: name,
      area: area,
      type: type,
      status: update.status,
      capacityMode: update.capacityMode,
      waitMinutes: update.waitMinutes,
      statusMessage: update.statusMessage,
      updatedAt: update.updatedAt,
      version: update.version,
      thumbnailUrl: thumbnailUrl,
      thumbnailAltText: thumbnailAltText,
    );
  }

  @override
  final String id;
  @override
  final String name;
  @override
  final String area;
  @override
  final AttractionType type;
  @override
  final AttractionStatus status;
  @override
  final AttractionCapacityMode capacityMode;
  @override
  final int? waitMinutes;
  @override
  final String? statusMessage;
  @override
  final DateTime updatedAt;
  @override
  final int version;
  final String? thumbnailUrl;
  final String? thumbnailAltText;

  @override
  List<Object?> get props => [
    id,
    name,
    area,
    type,
    status,
    capacityMode,
    waitMinutes,
    statusMessage,
    updatedAt,
    version,
    thumbnailUrl,
    thumbnailAltText,
  ];
}
