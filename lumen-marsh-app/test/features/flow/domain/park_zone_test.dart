import 'package:flutter_test/flutter_test.dart';
import 'package:lumen_marsh_app/features/flow/domain/park_zone.dart';

void main() {
  test('maps attractions onto static walking zones', () {
    expect(ParkZone.forAttraction('mangrove-run'), ParkZone.luminousWetlands);
    expect(ParkZone.forAttraction('cypress-coil'), ParkZone.cypressBasin);
    expect(
      ParkZone.forAttraction('stormglass-station'),
      ParkZone.researchQuarter,
    );
  });

  test('uses shorter walks inside a zone than across the park', () {
    expect(
      ParkZone.walkingMinutes(
        ParkZone.luminousWetlands.id,
        ParkZone.luminousWetlands.id,
      ),
      3,
    );
    expect(
      ParkZone.walkingMinutes(
        ParkZone.luminousWetlands.id,
        ParkZone.cypressBasin.id,
      ),
      8,
    );
    expect(
      ParkZone.walkingMinutes(
        ParkZone.luminousWetlands.id,
        ParkZone.researchQuarter.id,
      ),
      10,
    );
  });
}
