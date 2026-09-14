export '../../../core/venue/domain/venue_live.dart';

import 'api_enum.dart';

enum AttractionUpdateEventType {
  statusChanged,
  capacityChanged,
  waitTimeChanged;

  static AttractionUpdateEventType fromApi(String value) {
    return enumFromApi(AttractionUpdateEventType.values, value);
  }
}
