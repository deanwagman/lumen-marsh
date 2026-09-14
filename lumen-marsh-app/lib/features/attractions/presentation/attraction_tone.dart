import 'package:flutter/material.dart';

import '../../../design_system/components/status/lumen_tone.dart';
import '../domain/attraction.dart';

LumenTone toneForAttractionStatus(AttractionStatus status) {
  return switch (status) {
    AttractionStatus.operating => LumenTone.positive,
    AttractionStatus.closed => LumenTone.neutral,
    AttractionStatus.testing ||
    AttractionStatus.returningToService => LumenTone.warning,
    AttractionStatus.weatherHold => LumenTone.informational,
    AttractionStatus.technicalDelay => LumenTone.critical,
  };
}

IconData iconForAttractionStatus(AttractionStatus status) {
  return switch (status) {
    AttractionStatus.operating => Icons.check_circle_outline,
    AttractionStatus.closed => Icons.nightlight_outlined,
    AttractionStatus.testing => Icons.science_outlined,
    AttractionStatus.returningToService => Icons.restart_alt,
    AttractionStatus.weatherHold => Icons.thunderstorm_outlined,
    AttractionStatus.technicalDelay => Icons.build_outlined,
  };
}

String recommendedActionFor(AttractionStatus status) {
  return switch (status) {
    AttractionStatus.operating => 'Join the queue when you are ready.',
    AttractionStatus.closed => 'Check back later today.',
    AttractionStatus.testing => 'Stay nearby; explorers may be welcomed soon.',
    AttractionStatus.returningToService => 'Expect reopening shortly.',
    AttractionStatus.weatherHold => 'Seek indoor shelter until weather clears.',
    AttractionStatus.technicalDelay =>
      'Choose another attraction while crews recover the ride.',
  };
}

IconData placeholderIconFor(AttractionType type) {
  return switch (type) {
    AttractionType.boatExpedition => Icons.directions_boat_outlined,
    AttractionType.indoorDarkRide => Icons.theaters_outlined,
    AttractionType.launchCoaster => Icons.moving,
  };
}
