import '../../attractions/domain/api_enum.dart';

enum AdvisorySeverity {
  advisory,
  minor,
  major,
  critical;

  String get label => switch (this) {
    AdvisorySeverity.advisory => 'Advisory',
    AdvisorySeverity.minor => 'Minor',
    AdvisorySeverity.major => 'Major',
    AdvisorySeverity.critical => 'Critical',
  };

  static AdvisorySeverity fromApi(String value) {
    return enumFromApi(AdvisorySeverity.values, value);
  }
}

enum AdvisoryUpdateEventType {
  published,
  updated,
  withdrawn;

  static AdvisoryUpdateEventType fromApi(String value) {
    return enumFromApi(AdvisoryUpdateEventType.values, value);
  }
}
