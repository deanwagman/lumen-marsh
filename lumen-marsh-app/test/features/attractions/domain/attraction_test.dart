import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:lumen_marsh_app/features/attractions/domain/attraction_detail.dart';
import 'package:lumen_marsh_app/features/attractions/domain/attraction_enums.dart';
import 'package:lumen_marsh_app/features/attractions/domain/attraction_summary.dart';

import '../../../helpers/seeded_attractions.dart';

void main() {
  group('AttractionSummary', () {
    test('parses a guest list API response', () {
      final json = jsonDecode(mangroveRunJson) as Map<String, dynamic>;
      final summary = AttractionSummary.fromJson(json);

      expect(summary.id, 'mangrove-run');
      expect(summary.name, 'Mangrove Run');
      expect(summary.type, AttractionType.boatExpedition);
      expect(summary.waitMinutes, 25);
    });

    test('parses non-operating attractions with a null wait time', () {
      final json = jsonDecode(seededAttractionsJson) as List<dynamic>;
      final summaries = json
          .map(
            (item) => AttractionSummary.fromJson(item as Map<String, dynamic>),
          )
          .toList();

      expect(summaries[1].waitMinutes, isNull);
      expect(summaries[1].status, AttractionStatus.closed);
    });
  });

  group('AttractionDetail', () {
    test('parses operational and experience fields', () {
      final json = jsonDecode(mangroveRunDetailJson) as Map<String, dynamic>;
      final detail = AttractionDetail.fromJson(json);

      expect(detail.id, 'mangrove-run');
      expect(detail.waitMinutes, 25);
      expect(
        detail.experience.shortDescription,
        'Glide beneath a living canopy through the luminous wetlands.',
      );
      expect(detail.experience.durationMinutes, 8);
      expect(detail.experience.minimumHeightInches, isNull);
      expect(detail.experience.intensity, Intensity.gentle);
      expect(detail.experience.environment, Environment.outdoor);
      expect(detail.experience.heightRequirementLabel, 'No minimum height');
      expect(
        detail.experience.media.heroUrl,
        '/media/attractions/mangrove-run/hero.webp',
      );
      expect(
        detail.experience.media.altText,
        'An expedition boat moving through a glowing mangrove forest.',
      );
    });

    test('parses populated height requirements', () {
      final json = jsonDecode(mangroveRunDetailJson) as Map<String, dynamic>;
      final experience = Map<String, dynamic>.from(json['experience'] as Map);
      experience['minimumHeightInches'] = 48;
      json['experience'] = experience;

      final detail = AttractionDetail.fromJson(json);

      expect(detail.experience.minimumHeightInches, 48);
      expect(detail.experience.heightRequirementLabel, '48 in minimum');
    });
  });
}
