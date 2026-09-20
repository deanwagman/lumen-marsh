import 'package:equatable/equatable.dart';

class ParkZone extends Equatable {
  const ParkZone({required this.id, required this.name});

  static const luminousWetlandsId = 'luminous-wetlands';
  static const researchQuarterId = 'research-quarter';
  static const cypressBasinId = 'cypress-basin';

  static const luminousWetlands = ParkZone(
    id: luminousWetlandsId,
    name: 'Luminous Wetlands',
  );
  static const researchQuarter = ParkZone(
    id: researchQuarterId,
    name: 'Research Quarter',
  );
  static const cypressBasin = ParkZone(
    id: cypressBasinId,
    name: 'Cypress Basin',
  );
  static const park = ParkZone(id: 'park', name: 'Park');

  static const all = [luminousWetlands, researchQuarter, cypressBasin];

  static const _attractionZones = {
    'mangrove-run': luminousWetlands,
    'stormglass-station': researchQuarter,
    'cypress-coil': cypressBasin,
  };

  final String id;
  final String name;

  static ParkZone forAttraction(String attractionId) {
    return _attractionZones[attractionId] ?? park;
  }

  static ParkZone forId(String zoneId) {
    for (final zone in all) {
      if (zone.id == zoneId) {
        return zone;
      }
    }
    return park;
  }

  static int walkingMinutes(String fromZoneId, String toZoneId) {
    if (fromZoneId == toZoneId) {
      return 3;
    }
    final pair = {fromZoneId, toZoneId};
    if (pair.contains(luminousWetlands.id) && pair.contains(cypressBasin.id)) {
      return 8;
    }
    if (pair.contains(luminousWetlands.id) &&
        pair.contains(researchQuarter.id)) {
      return 10;
    }
    return 12;
  }

  @override
  List<Object?> get props => [id, name];
}
