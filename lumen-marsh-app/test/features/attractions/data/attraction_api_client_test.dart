import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:lumen_marsh_app/core/errors/app_failure.dart';
import 'package:lumen_marsh_app/features/attractions/data/attraction_api_client.dart';
import 'package:lumen_marsh_app/features/attractions/domain/attraction_enums.dart';

import '../../../helpers/seeded_attractions.dart';

void main() {
  test('fetchAll maps a catalog payload into summary objects', () async {
    final client = AttractionApiClient(
      client: MockClient((request) async {
        expect(
          request.url.toString(),
          'http://localhost:8080/api/v1/attractions',
        );
        expect(request.headers['Accept'], 'application/json');
        return http.Response(seededAttractionsJson, 200);
      }),
      baseUrl: 'http://localhost:8080',
    );

    final attractions = await client.fetchAll();

    expect(attractions, hasLength(3));
    expect(attractions.first.name, 'Mangrove Run');
    expect(attractions.first.status, AttractionStatus.operating);
    expect(
      attractions.first.thumbnailUrl,
      '/media/attractions/mangrove-run/thumbnail.webp',
    );
    expect(
      attractions.first.thumbnailAltText,
      'An expedition boat moving through a glowing mangrove forest.',
    );
  });

  test('fetchById maps a detail payload with experience', () async {
    final client = AttractionApiClient(
      client: MockClient((request) async {
        expect(
          request.url.toString(),
          'http://localhost:8080/api/v1/attractions/mangrove-run',
        );
        return http.Response(mangroveRunDetailJson, 200);
      }),
      baseUrl: 'http://localhost:8080',
    );

    final detail = await client.fetchById('mangrove-run');

    expect(detail.id, 'mangrove-run');
    expect(detail.waitMinutes, 25);
    expect(detail.experience.durationMinutes, 8);
    expect(detail.experience.minimumHeightInches, isNull);
    expect(
      detail.experience.accessibilitySummary,
      'Guests must transfer into the ride vehicle.',
    );
  });

  test(
    'fetchAll throws NetworkFailure when the host cannot be reached',
    () async {
      final client = AttractionApiClient(
        client: MockClient((request) async {
          throw http.ClientException('Failed to fetch', request.url);
        }),
        baseUrl: 'http://localhost:8080',
      );

      expect(client.fetchAll(), throwsA(isA<NetworkFailure>()));
    },
  );

  test('fetchById throws NotFoundFailure on a 404', () async {
    final client = AttractionApiClient(
      client: MockClient((request) async {
        return http.Response('{"code":"ATTRACTION_NOT_FOUND"}', 404);
      }),
      baseUrl: 'http://localhost:8080',
    );

    expect(client.fetchById('missing-ride'), throwsA(isA<NotFoundFailure>()));
  });
}
