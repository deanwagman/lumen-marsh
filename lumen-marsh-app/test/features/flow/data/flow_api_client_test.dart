import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:lumen_marsh_app/core/errors/app_failure.dart';
import 'package:lumen_marsh_app/features/flow/data/flow_api_client.dart';
import 'package:lumen_marsh_app/features/flow/domain/guest_wait.dart';

import '../../../helpers/seeded_flow.dart';

void main() {
  test('fetchOverview maps guest-safe wait snapshots', () async {
    final client = FlowApiClient(
      client: MockClient((request) async {
        expect(
          request.url.toString(),
          'http://localhost:8080/api/v1/flow/overview',
        );
        return http.Response(guestFlowOverviewJson, 200);
      }),
      baseUrl: 'http://localhost:8080',
    );

    final overview = await client.fetchOverview();

    expect(overview.attractions, hasLength(2));
    expect(overview.attractions.first.attractionId, 'mangrove-run');
    expect(overview.attractions.first.trend, GuestWaitTrend.likelyStable);
    expect(overview.simulated, isTrue);
    expect(overview.attractions.first.forecast30Minutes, 'About 25 minutes');
  });

  test('fetchWaitForecast maps a single attraction outlook', () async {
    final client = FlowApiClient(
      client: MockClient((request) async {
        expect(
          request.url.toString(),
          'http://localhost:8080/api/v1/attractions/mangrove-run/wait-forecast',
        );
        return http.Response(guestWaitJson, 200);
      }),
      baseUrl: 'http://localhost:8080',
    );

    final wait = await client.fetchWaitForecast('mangrove-run');

    expect(wait.displayName, 'Mangrove Run');
    expect(wait.freshness, QueueFreshness.fresh);
  });

  test(
    'fetchOverview throws NetworkFailure when the host cannot be reached',
    () async {
      final client = FlowApiClient(
        client: MockClient((request) async {
          throw http.ClientException('Failed to fetch', request.url);
        }),
        baseUrl: 'http://localhost:8080',
      );

      expect(client.fetchOverview(), throwsA(isA<NetworkFailure>()));
    },
  );
}
