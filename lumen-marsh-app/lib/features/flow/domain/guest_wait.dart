import 'package:equatable/equatable.dart';

import '../../attractions/domain/api_enum.dart';

enum QueueFreshness {
  fresh,
  delayed,
  stale;

  String get label => switch (this) {
    QueueFreshness.fresh => 'Fresh',
    QueueFreshness.delayed => 'Delayed',
    QueueFreshness.stale => 'Stale',
  };

  static QueueFreshness fromApi(String value) {
    return enumFromApi(QueueFreshness.values, value);
  }
}

enum GuestWaitTrend {
  likelyRising,
  likelyFalling,
  likelyStable;

  String get label => switch (this) {
    GuestWaitTrend.likelyRising => 'Likely rising',
    GuestWaitTrend.likelyFalling => 'Likely falling',
    GuestWaitTrend.likelyStable => 'Likely stable',
  };

  String get expectedPhrase => switch (this) {
    GuestWaitTrend.likelyRising => 'rise',
    GuestWaitTrend.likelyFalling => 'fall',
    GuestWaitTrend.likelyStable => 'remain stable',
  };

  static GuestWaitTrend fromApi(String value) {
    return enumFromApi(GuestWaitTrend.values, value);
  }
}

enum GuestAvailability {
  operating,
  unavailable;

  bool get isOperating => this == GuestAvailability.operating;

  static GuestAvailability fromApi(String value) {
    return enumFromApi(GuestAvailability.values, value);
  }
}

class GuestWait extends Equatable {
  const GuestWait({
    required this.attractionId,
    required this.displayName,
    required this.originZoneId,
    required this.originZoneName,
    required this.postedWaitMinutes,
    required this.waitOutlook,
    required this.forecast30Minutes,
    required this.trend,
    required this.availability,
    required this.freshness,
    required this.updatedAt,
    required this.simulated,
  });

  factory GuestWait.fromJson(Map<String, dynamic> json) {
    return GuestWait(
      attractionId: json['attractionId'] as String,
      displayName: json['displayName'] as String,
      originZoneId: json['originZoneId'] as String,
      originZoneName: json['originZoneName'] as String,
      postedWaitMinutes: (json['postedWaitMinutes'] as num?)?.toInt(),
      waitOutlook: json['waitOutlook'] as String?,
      forecast30Minutes: json['forecast30Minutes'] as String?,
      trend: GuestWaitTrend.fromApi(json['trend'] as String),
      availability: GuestAvailability.fromApi(json['availability'] as String),
      freshness: QueueFreshness.fromApi(json['freshness'] as String),
      updatedAt: DateTime.parse(json['updatedAt'] as String),
      simulated: json['simulated'] as bool? ?? false,
    );
  }

  GuestWait mergeLiveUpdate(GuestWait incoming) {
    if (incoming.updatedAt.isBefore(updatedAt)) {
      return this;
    }
    return GuestWait(
      attractionId: incoming.attractionId,
      displayName: incoming.displayName,
      originZoneId: incoming.originZoneId,
      originZoneName: incoming.originZoneName,
      postedWaitMinutes: incoming.postedWaitMinutes,
      waitOutlook: incoming.waitOutlook,
      forecast30Minutes: incoming.forecast30Minutes ?? forecast30Minutes,
      trend: incoming.trend,
      availability: incoming.availability,
      freshness: incoming.freshness,
      updatedAt: incoming.updatedAt,
      simulated: incoming.simulated,
    );
  }

  final String attractionId;
  final String displayName;
  final String originZoneId;
  final String originZoneName;
  final int? postedWaitMinutes;
  final String? waitOutlook;
  final String? forecast30Minutes;
  final GuestWaitTrend trend;
  final GuestAvailability availability;
  final QueueFreshness freshness;
  final DateTime updatedAt;
  final bool simulated;

  bool get isStale => freshness == QueueFreshness.stale;

  int? get forecastMinutes {
    final text = forecast30Minutes;
    if (text == null) {
      return null;
    }
    final match = RegExp(r'(\d+)').firstMatch(text);
    return match == null ? null : int.parse(match.group(1)!);
  }

  @override
  List<Object?> get props => [
    attractionId,
    displayName,
    originZoneId,
    originZoneName,
    postedWaitMinutes,
    waitOutlook,
    forecast30Minutes,
    trend,
    availability,
    freshness,
    updatedAt,
    simulated,
  ];
}

class GuestGuidance extends Equatable {
  const GuestGuidance({
    required this.recommendationId,
    required this.recommendedDestinationIds,
    required this.guestMessage,
    required this.updatedAt,
    required this.simulated,
  });

  factory GuestGuidance.fromJson(Map<String, dynamic> json) {
    return GuestGuidance(
      recommendationId: json['recommendationId'] as String,
      recommendedDestinationIds: [
        for (final id
            in json['recommendedDestinationIds'] as List<dynamic>? ?? const [])
          id as String,
      ],
      guestMessage: json['guestMessage'] as String?,
      updatedAt: DateTime.parse(json['updatedAt'] as String),
      simulated: json['simulated'] as bool? ?? false,
    );
  }

  final String recommendationId;
  final List<String> recommendedDestinationIds;
  final String? guestMessage;
  final DateTime updatedAt;
  final bool simulated;

  bool recommends(String attractionId) =>
      recommendedDestinationIds.contains(attractionId);

  @override
  List<Object?> get props => [
    recommendationId,
    recommendedDestinationIds,
    guestMessage,
    updatedAt,
    simulated,
  ];
}

class GuestFlowOverview extends Equatable {
  const GuestFlowOverview({
    required this.attractions,
    required this.publishedGuidance,
    required this.updatedAt,
    required this.simulated,
  });

  factory GuestFlowOverview.fromJson(Map<String, dynamic> json) {
    return GuestFlowOverview(
      attractions: [
        for (final item in json['attractions'] as List<dynamic>)
          GuestWait.fromJson(Map<String, dynamic>.from(item as Map)),
      ],
      publishedGuidance: [
        for (final item
            in json['publishedGuidance'] as List<dynamic>? ?? const [])
          GuestGuidance.fromJson(Map<String, dynamic>.from(item as Map)),
      ],
      updatedAt: DateTime.parse(json['updatedAt'] as String),
      simulated: json['simulated'] as bool? ?? false,
    );
  }

  final List<GuestWait> attractions;
  final List<GuestGuidance> publishedGuidance;
  final DateTime updatedAt;
  final bool simulated;

  GuestWait? waitFor(String attractionId) {
    for (final wait in attractions) {
      if (wait.attractionId == attractionId) {
        return wait;
      }
    }
    return null;
  }

  @override
  List<Object?> get props => [
    attractions,
    publishedGuidance,
    updatedAt,
    simulated,
  ];
}
