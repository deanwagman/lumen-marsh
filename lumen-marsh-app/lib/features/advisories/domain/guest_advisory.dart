import 'package:equatable/equatable.dart';

import 'advisory_severity.dart';

class GuestAdvisory extends Equatable {
  const GuestAdvisory({
    required this.id,
    required this.severity,
    required this.title,
    required this.message,
    required this.affectedAttractionIds,
    required this.updatedAt,
    required this.version,
  });

  factory GuestAdvisory.fromJson(Map<String, dynamic> json) {
    return GuestAdvisory(
      id: json['id'] as String,
      severity: AdvisorySeverity.fromApi(json['severity'] as String),
      title: json['title'] as String,
      message: json['message'] as String,
      affectedAttractionIds: [
        for (final id in json['affectedAttractionIds'] as List<dynamic>)
          id as String,
      ],
      updatedAt: DateTime.parse(json['updatedAt'] as String),
      version: (json['version'] as num).toInt(),
    );
  }

  factory GuestAdvisory.fromOperationalJson(Map<String, dynamic> json) {
    final advisory = Map<String, dynamic>.from(json['advisory'] as Map);
    return GuestAdvisory.fromJson(advisory);
  }

  final String id;
  final AdvisorySeverity severity;
  final String title;
  final String message;
  final List<String> affectedAttractionIds;
  final DateTime updatedAt;
  final int version;

  bool affects(String attractionId) =>
      affectedAttractionIds.contains(attractionId);

  @override
  List<Object?> get props => [
    id,
    severity,
    title,
    message,
    affectedAttractionIds,
    updatedAt,
    version,
  ];
}
