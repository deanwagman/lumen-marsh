import 'package:equatable/equatable.dart';

import 'attraction_enums.dart';
import 'attraction_media.dart';

class AttractionExperience extends Equatable {
  const AttractionExperience({
    required this.shortDescription,
    required this.durationMinutes,
    required this.minimumHeightInches,
    required this.intensity,
    required this.environment,
    required this.singleRiderAvailable,
    required this.accessibilitySummary,
    required this.media,
  });

  factory AttractionExperience.fromJson(Map<String, dynamic> json) {
    return AttractionExperience(
      shortDescription: json['shortDescription'] as String,
      durationMinutes: (json['durationMinutes'] as num).toInt(),
      minimumHeightInches: (json['minimumHeightInches'] as num?)?.toInt(),
      intensity: Intensity.fromApi(json['intensity'] as String),
      environment: Environment.fromApi(json['environment'] as String),
      singleRiderAvailable: json['singleRiderAvailable'] as bool,
      accessibilitySummary: json['accessibilitySummary'] as String,
      media: AttractionMedia.fromJson(
        Map<String, dynamic>.from(json['media'] as Map),
      ),
    );
  }

  final String shortDescription;
  final int durationMinutes;
  final int? minimumHeightInches;
  final Intensity intensity;
  final Environment environment;
  final bool singleRiderAvailable;
  final String accessibilitySummary;
  final AttractionMedia media;

  String get durationLabel => '$durationMinutes minutes';

  String get heightRequirementLabel {
    final inches = minimumHeightInches;
    if (inches == null) {
      return 'No minimum height';
    }
    return '$inches in minimum';
  }

  @override
  List<Object?> get props => [
    shortDescription,
    durationMinutes,
    minimumHeightInches,
    intensity,
    environment,
    singleRiderAvailable,
    accessibilitySummary,
    media,
  ];
}
