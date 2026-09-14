import 'package:flutter_test/flutter_test.dart';
import 'package:lumen_marsh_app/design_system/components/status/lumen_tone.dart';
import 'package:lumen_marsh_app/features/attractions/domain/attraction.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/attraction_tone.dart';

void main() {
  test('maps attraction statuses onto semantic tones', () {
    expect(
      toneForAttractionStatus(AttractionStatus.operating),
      LumenTone.positive,
    );
    expect(toneForAttractionStatus(AttractionStatus.closed), LumenTone.neutral);
    expect(
      toneForAttractionStatus(AttractionStatus.testing),
      LumenTone.warning,
    );
    expect(
      toneForAttractionStatus(AttractionStatus.returningToService),
      LumenTone.warning,
    );
    expect(
      toneForAttractionStatus(AttractionStatus.weatherHold),
      LumenTone.informational,
    );
    expect(
      toneForAttractionStatus(AttractionStatus.technicalDelay),
      LumenTone.critical,
    );
  });
}
