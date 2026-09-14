import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:lumen_marsh_app/core/venue/data/venue_event_hub.dart';
import 'package:lumen_marsh_app/features/attractions/data/attraction_api_client.dart';
import 'package:lumen_marsh_app/core/venue/domain/venue_stream_message.dart';
import 'package:lumen_marsh_app/features/attractions/data/attraction_repository.dart';

import '../../../helpers/seeded_attractions.dart';
import '../../../helpers/venue_live_test_support.dart';

class _MockApiClient extends Mock implements AttractionApiClient {}

class _FakeVenueHub extends Mock implements VenueEventHub {}

void main() {
  late _MockApiClient apiClient;
  late _FakeVenueHub hub;
  late HttpAttractionRepository repository;

  setUp(() {
    apiClient = _MockApiClient();
    hub = _FakeVenueHub();
    when(() => apiClient.baseUrl).thenReturn('http://localhost:8080');
    repository = HttpAttractionRepository(apiClient, hub);
  });

  test('list returns domain objects from the API client', () async {
    when(() => apiClient.fetchAll()).thenAnswer((_) async => seededAttractions);

    final attractions = await repository.list();

    expect(attractions, seededAttractions);
    verify(() => apiClient.fetchAll()).called(1);
  });

  test('getById returns the matching domain object', () async {
    when(() => apiClient.fetchById('mangrove-run'))
        .thenAnswer((_) async => mangroveRunDetail);

    final detail = await repository.getById('mangrove-run');

    expect(detail, mangroveRunDetail);
  });

  test('watchUpdates shares one venue hub stream', () async {
    final feed = FakeVenueLiveFeed();
    stubVenueLiveFeed(hub, feed);

    final first = repository.watchUpdates();
    final second = repository.watchUpdates();

    final values = <Object>[];
    final sub1 = first.listen(values.add);
    final sub2 = second.listen(values.add);

    feed.add(AttractionsSnapshotMessage(seededAttractions));
    await Future<void>.delayed(Duration.zero);

    expect(feed.listenCount, greaterThanOrEqualTo(1));
    expect(values, hasLength(2));
    await sub1.cancel();
    await sub2.cancel();
    await feed.close();
  });

  test('dispose clears local broadcast without closing shared hub', () async {
    when(() => repository.watchUpdates()).thenAnswer(
      (_) => StreamController<VenueStreamMessage>.broadcast().stream,
    );
    await repository.dispose();
    verifyNever(() => hub.dispose());
  });
}
