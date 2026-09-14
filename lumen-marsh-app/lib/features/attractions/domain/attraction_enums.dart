import 'package:equatable/equatable.dart';

import 'api_enum.dart';

enum AttractionType {
  boatExpedition,
  indoorDarkRide,
  launchCoaster;

  String get label => switch (this) {
    AttractionType.boatExpedition => 'Boat expedition',
    AttractionType.indoorDarkRide => 'Indoor dark ride',
    AttractionType.launchCoaster => 'Launch coaster',
  };

  static AttractionType fromApi(String value) {
    return enumFromApi(AttractionType.values, value);
  }
}

enum AttractionStatus {
  closed,
  testing,
  returningToService,
  operating,
  weatherHold,
  technicalDelay;

  String get label => switch (this) {
    AttractionStatus.closed => 'Closed',
    AttractionStatus.testing => 'Testing',
    AttractionStatus.returningToService => 'Returning to service',
    AttractionStatus.operating => 'Operating',
    AttractionStatus.weatherHold => 'Weather hold',
    AttractionStatus.technicalDelay => 'Technical delay',
  };

  bool get isOperating => this == AttractionStatus.operating;

  static AttractionStatus fromApi(String value) {
    return enumFromApi(AttractionStatus.values, value);
  }
}

enum AttractionCapacityMode {
  notApplicable,
  normal,
  reduced;

  String get label => switch (this) {
    AttractionCapacityMode.notApplicable => 'Not applicable',
    AttractionCapacityMode.normal => 'Normal',
    AttractionCapacityMode.reduced => 'Reduced',
  };

  static AttractionCapacityMode fromApi(String value) {
    return enumFromApi(AttractionCapacityMode.values, value);
  }
}

enum Intensity {
  gentle,
  moderate,
  thrill;

  String get label => switch (this) {
    Intensity.gentle => 'Gentle',
    Intensity.moderate => 'Moderate',
    Intensity.thrill => 'Thrill',
  };

  static Intensity fromApi(String value) {
    return enumFromApi(Intensity.values, value);
  }
}

enum Environment {
  indoor,
  outdoor,
  mixed;

  String get label => switch (this) {
    Environment.indoor => 'Indoor',
    Environment.outdoor => 'Outdoor',
    Environment.mixed => 'Mixed',
  };

  static Environment fromApi(String value) {
    return enumFromApi(Environment.values, value);
  }
}

mixin AttractionOperationalFields on Equatable {
  String get id;
  String get name;
  String get area;
  AttractionType get type;
  AttractionStatus get status;
  AttractionCapacityMode get capacityMode;
  int? get waitMinutes;
  String? get statusMessage;
  DateTime get updatedAt;
  int get version;

  String get waitTimeLabel {
    final minutes = waitMinutes;
    if (minutes == null) {
      return 'Wait not posted';
    }
    if (minutes == 0) {
      return 'Walk on';
    }
    return '$minutes min wait';
  }

  String get capacityLabel => switch (capacityMode) {
    AttractionCapacityMode.notApplicable => 'Capacity not applicable',
    AttractionCapacityMode.normal => 'Normal capacity',
    AttractionCapacityMode.reduced => 'Reduced capacity',
  };
}
