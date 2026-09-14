import 'package:lumen_marsh_app/features/attractions/domain/attraction_detail.dart';
import 'package:lumen_marsh_app/features/attractions/domain/attraction_enums.dart';
import 'package:lumen_marsh_app/features/attractions/domain/attraction_experience.dart';
import 'package:lumen_marsh_app/features/attractions/domain/attraction_media.dart';
import 'package:lumen_marsh_app/features/attractions/domain/attraction_summary.dart';

const _mangroveThumbnailUrl = '/media/attractions/mangrove-run/thumbnail.webp';
const _mangroveThumbnailAlt =
    'An expedition boat moving through a glowing mangrove forest.';
const _stormglassThumbnailUrl =
    '/media/attractions/stormglass-station/thumbnail.webp';
const _stormglassThumbnailAlt =
    'A glass-walled research station glowing with stormglass instruments.';
const _cypressThumbnailUrl = '/media/attractions/cypress-coil/thumbnail.webp';
const _cypressThumbnailAlt =
    'A launch coaster spiraling through a cypress basin canopy.';

final mangroveRunSummary = AttractionSummary(
  id: 'mangrove-run',
  name: 'Mangrove Run',
  area: 'Luminous Wetlands',
  type: AttractionType.boatExpedition,
  status: AttractionStatus.operating,
  capacityMode: AttractionCapacityMode.normal,
  waitMinutes: 25,
  statusMessage: null,
  updatedAt: DateTime.parse('2026-08-26T18:42:00Z'),
  version: 0,
  thumbnailUrl: _mangroveThumbnailUrl,
  thumbnailAltText: _mangroveThumbnailAlt,
);

final mangroveRunExperience = AttractionExperience(
  shortDescription:
      'Glide beneath a living canopy through the luminous wetlands.',
  durationMinutes: 8,
  minimumHeightInches: null,
  intensity: Intensity.gentle,
  environment: Environment.outdoor,
  singleRiderAvailable: false,
  accessibilitySummary: 'Guests must transfer into the ride vehicle.',
  media: const AttractionMedia(
    heroUrl: '/media/attractions/mangrove-run/hero.webp',
    thumbnailUrl: _mangroveThumbnailUrl,
    altText: _mangroveThumbnailAlt,
  ),
);

final mangroveRunDetail = AttractionDetail(
  summary: mangroveRunSummary,
  experience: mangroveRunExperience,
);

final stormglassStation = AttractionSummary(
  id: 'stormglass-station',
  name: 'Stormglass Station',
  area: 'Research Quarter',
  type: AttractionType.indoorDarkRide,
  status: AttractionStatus.closed,
  capacityMode: AttractionCapacityMode.notApplicable,
  waitMinutes: null,
  statusMessage: 'Currently closed.',
  updatedAt: DateTime.parse('2026-08-26T18:42:00Z'),
  version: 0,
  thumbnailUrl: _stormglassThumbnailUrl,
  thumbnailAltText: _stormglassThumbnailAlt,
);

final cypressCoil = AttractionSummary(
  id: 'cypress-coil',
  name: 'Cypress Coil',
  area: 'Cypress Basin',
  type: AttractionType.launchCoaster,
  status: AttractionStatus.closed,
  capacityMode: AttractionCapacityMode.notApplicable,
  waitMinutes: null,
  statusMessage: 'Currently closed.',
  updatedAt: DateTime.parse('2026-08-26T18:42:00Z'),
  version: 0,
  thumbnailUrl: _cypressThumbnailUrl,
  thumbnailAltText: _cypressThumbnailAlt,
);

final seededAttractions = [mangroveRunSummary, stormglassStation, cypressCoil];

const mangroveRunJson = '''
{
  "id": "mangrove-run",
  "name": "Mangrove Run",
  "area": "Luminous Wetlands",
  "type": "BOAT_EXPEDITION",
  "status": "OPERATING",
  "capacityMode": "NORMAL",
  "waitMinutes": 25,
  "statusMessage": null,
  "updatedAt": "2026-08-26T18:42:00Z",
  "version": 0,
  "thumbnailUrl": "/media/attractions/mangrove-run/thumbnail.webp",
  "thumbnailAltText": "An expedition boat moving through a glowing mangrove forest."
}
''';

const mangroveRunDetailJson = '''
{
  "id": "mangrove-run",
  "name": "Mangrove Run",
  "area": "Luminous Wetlands",
  "type": "BOAT_EXPEDITION",
  "status": "OPERATING",
  "capacityMode": "NORMAL",
  "waitMinutes": 25,
  "statusMessage": "Currently operating.",
  "updatedAt": "2026-08-27T14:30:00Z",
  "version": 0,
  "thumbnailUrl": "/media/attractions/mangrove-run/thumbnail.webp",
  "thumbnailAltText": "An expedition boat moving through a glowing mangrove forest.",
  "experience": {
    "shortDescription": "Glide beneath a living canopy through the luminous wetlands.",
    "durationMinutes": 8,
    "minimumHeightInches": null,
    "intensity": "GENTLE",
    "environment": "OUTDOOR",
    "singleRiderAvailable": false,
    "accessibilitySummary": "Guests must transfer into the ride vehicle.",
    "media": {
      "heroUrl": "/media/attractions/mangrove-run/hero.webp",
      "thumbnailUrl": "/media/attractions/mangrove-run/thumbnail.webp",
      "altText": "An expedition boat moving through a glowing mangrove forest."
    }
  }
}
''';

const seededAttractionsJson = '''
[
  {
    "id": "mangrove-run",
    "name": "Mangrove Run",
    "area": "Luminous Wetlands",
    "type": "BOAT_EXPEDITION",
    "status": "OPERATING",
    "capacityMode": "NORMAL",
    "waitMinutes": 25,
    "statusMessage": null,
    "updatedAt": "2026-08-26T18:42:00Z",
    "version": 0,
    "thumbnailUrl": "/media/attractions/mangrove-run/thumbnail.webp",
    "thumbnailAltText": "An expedition boat moving through a glowing mangrove forest."
  },
  {
    "id": "stormglass-station",
    "name": "Stormglass Station",
    "area": "Research Quarter",
    "type": "INDOOR_DARK_RIDE",
    "status": "CLOSED",
    "capacityMode": "NOT_APPLICABLE",
    "waitMinutes": null,
    "statusMessage": "Currently closed.",
    "updatedAt": "2026-08-26T18:42:00Z",
    "version": 0,
    "thumbnailUrl": "/media/attractions/stormglass-station/thumbnail.webp",
    "thumbnailAltText": "A glass-walled research station glowing with stormglass instruments."
  },
  {
    "id": "cypress-coil",
    "name": "Cypress Coil",
    "area": "Cypress Basin",
    "type": "LAUNCH_COASTER",
    "status": "CLOSED",
    "capacityMode": "NOT_APPLICABLE",
    "waitMinutes": null,
    "statusMessage": "Currently closed.",
    "updatedAt": "2026-08-26T18:42:00Z",
    "version": 0,
    "thumbnailUrl": "/media/attractions/cypress-coil/thumbnail.webp",
    "thumbnailAltText": "A launch coaster spiraling through a cypress basin canopy."
  }
]
''';
