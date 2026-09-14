import '../../features/attractions/domain/attraction_detail.dart';
import '../../features/attractions/domain/attraction_enums.dart';
import '../../features/attractions/domain/attraction_experience.dart';
import '../../features/attractions/domain/attraction_media.dart';
import '../../features/attractions/domain/attraction_summary.dart';

final galleryMangroveRun = AttractionSummary(
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
);

final galleryMangroveRunDetail = AttractionDetail(
  summary: galleryMangroveRun,
  experience: const AttractionExperience(
    shortDescription:
        'Glide beneath a living canopy through the luminous wetlands.',
    durationMinutes: 8,
    minimumHeightInches: null,
    intensity: Intensity.gentle,
    environment: Environment.outdoor,
    singleRiderAvailable: false,
    accessibilitySummary: 'Guests must transfer into the ride vehicle.',
    media: AttractionMedia(
      heroUrl: '/media/attractions/mangrove-run/hero.webp',
      thumbnailUrl: '/media/attractions/mangrove-run/thumbnail.webp',
      altText: 'An expedition boat moving through a glowing mangrove forest.',
    ),
  ),
);

final galleryWeatherHold = AttractionSummary(
  id: 'mangrove-run',
  name: 'Mangrove Run',
  area: 'Luminous Wetlands',
  type: AttractionType.boatExpedition,
  status: AttractionStatus.weatherHold,
  capacityMode: AttractionCapacityMode.notApplicable,
  waitMinutes: null,
  statusMessage: 'Temporarily unavailable due to nearby weather.',
  updatedAt: DateTime.parse('2026-08-26T18:42:00Z'),
  version: 1,
);
