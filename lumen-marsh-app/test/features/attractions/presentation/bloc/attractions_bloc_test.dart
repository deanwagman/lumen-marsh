import 'package:bloc_test/bloc_test.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:lumen_marsh_app/core/errors/app_failure.dart';
import 'package:lumen_marsh_app/features/attractions/data/attraction_repository.dart';
import 'package:lumen_marsh_app/features/attractions/domain/attraction_enums.dart';
import 'package:lumen_marsh_app/features/attractions/domain/attraction_live.dart';
import 'package:lumen_marsh_app/features/attractions/domain/attraction_operational_update.dart';
import 'package:lumen_marsh_app/core/venue/domain/venue_stream_message.dart';
import 'package:lumen_marsh_app/features/attractions/domain/attraction_summary.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/bloc/attractions_bloc.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/bloc/attractions_event.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/bloc/attractions_state.dart';

import '../../../../helpers/venue_live_test_support.dart';
import '../../../../helpers/seeded_attractions.dart';

class _MockAttractionRepository extends Mock implements AttractionRepository {}

AttractionOperationalUpdate mangroveWaitUpdate({
  required int version,
  required int waitMinutes,
}) {
  return AttractionOperationalUpdate(
    eventId: 'evt-$version',
    eventType: AttractionUpdateEventType.waitTimeChanged,
    occurredAt: DateTime.parse('2026-08-27T15:00:00Z'),
    attractionId: 'mangrove-run',
    status: AttractionStatus.operating,
    capacityMode: AttractionCapacityMode.normal,
    waitMinutes: waitMinutes,
    statusMessage: null,
    updatedAt: DateTime.parse('2026-08-27T15:00:00Z'),
    version: version,
  );
}

AttractionSummary mangroveAt({required int version, required int waitMinutes}) {
  return mangroveRunSummary.applyOperationalUpdate(
    mangroveWaitUpdate(version: version, waitMinutes: waitMinutes),
  );
}

void main() {
  late _MockAttractionRepository repository;
  late FakeVenueLiveFeed feed;

  setUp(() {
    repository = _MockAttractionRepository();
    feed = FakeVenueLiveFeed();
    stubAttractionLiveFeed(repository, feed);
  });

  tearDown(() async {
    await feed.close();
  });

  blocTest<AttractionsBloc, AttractionsState>(
    'emits loading followed by loaded and starts live updates',
    build: () {
      when(() => repository.list()).thenAnswer((_) async => seededAttractions);
      return AttractionsBloc(repository: repository);
    },
    act: (bloc) => bloc.add(const AttractionsRequested()),
    expect: () => [
      isA<AttractionsLoading>(),
      isA<AttractionsLoaded>()
          .having((s) => s.attractions, 'attractions', seededAttractions)
          .having(
            (s) => s.connectionStatus,
            'connection',
            LiveConnectionStatus.connecting,
          ),
    ],
    verify: (_) {
      verify(() => repository.watchUpdates()).called(1);
    },
  );

  blocTest<AttractionsBloc, AttractionsState>(
    'emits failure after a network error',
    build: () {
      when(() => repository.list()).thenThrow(const NetworkFailure());
      return AttractionsBloc(repository: repository);
    },
    act: (bloc) => bloc.add(const AttractionsRequested()),
    expect: () => [
      const AttractionsLoading(),
      const AttractionsFailure(NetworkFailure()),
    ],
  );

  blocTest<AttractionsBloc, AttractionsState>(
    'emits empty when the catalog has no attractions',
    build: () {
      when(() => repository.list()).thenAnswer((_) async => []);
      return AttractionsBloc(repository: repository);
    },
    act: (bloc) => bloc.add(const AttractionsRequested()),
    expect: () => const [AttractionsLoading(), AttractionsEmpty()],
  );

  blocTest<AttractionsBloc, AttractionsState>(
    'applies a newer live update to the matching attraction only',
    build: () {
      when(() => repository.list()).thenAnswer((_) async => seededAttractions);
      return AttractionsBloc(repository: repository);
    },
    act: (bloc) async {
      bloc.add(const AttractionsRequested());
      await pumpEventQueue();
      feed.add(
        AttractionUpdatedMessage(
          mangroveWaitUpdate(version: 1, waitMinutes: 40),
        ),
      );
      await pumpEventQueue();
    },
    expect: () => [
      isA<AttractionsLoading>(),
      isA<AttractionsLoaded>(),
      isA<AttractionsLoaded>()
          .having((s) => s.attractions.first.waitMinutes, 'mangrove wait', 40)
          .having((s) => s.attractions[1], 'stormglass', stormglassStation)
          .having(
            (s) => s.connectionStatus,
            'connection',
            LiveConnectionStatus.connected,
          ),
    ],
  );

  blocTest<AttractionsBloc, AttractionsState>(
    'ignores duplicate and older versions',
    build: () {
      when(() => repository.list()).thenAnswer(
        (_) async => [
          mangroveAt(version: 2, waitMinutes: 40),
          stormglassStation,
          cypressCoil,
        ],
      );
      return AttractionsBloc(repository: repository);
    },
    act: (bloc) async {
      bloc.add(const AttractionsRequested());
      await pumpEventQueue();
      feed.add(
        AttractionUpdatedMessage(
          mangroveWaitUpdate(version: 2, waitMinutes: 55),
        ),
      );
      feed.add(
        AttractionUpdatedMessage(
          mangroveWaitUpdate(version: 1, waitMinutes: 10),
        ),
      );
      await pumpEventQueue();
    },
    expect: () => [
      isA<AttractionsLoading>(),
      isA<AttractionsLoaded>().having(
        (s) => s.attractions.first.waitMinutes,
        'wait',
        40,
      ),
    ],
  );

  blocTest<AttractionsBloc, AttractionsState>(
    'does not regress newer local state with an older snapshot',
    build: () {
      when(() => repository.list()).thenAnswer(
        (_) async => [
          mangroveAt(version: 5, waitMinutes: 40),
          stormglassStation,
          cypressCoil,
        ],
      );
      return AttractionsBloc(repository: repository);
    },
    act: (bloc) async {
      bloc.add(const AttractionsRequested());
      await pumpEventQueue();
      feed.add(
        AttractionsSnapshotMessage([
          mangroveAt(version: 3, waitMinutes: 25),
          stormglassStation,
          cypressCoil,
        ]),
      );
      await pumpEventQueue();
    },
    expect: () => [
      isA<AttractionsLoading>(),
      isA<AttractionsLoaded>(),
      isA<AttractionsLoaded>()
          .having((s) => s.attractions.first.version, 'version', 5)
          .having((s) => s.attractions.first.waitMinutes, 'wait', 40)
          .having(
            (s) => s.connectionStatus,
            'connection',
            LiveConnectionStatus.connected,
          ),
    ],
  );

  blocTest<AttractionsBloc, AttractionsState>(
    'unknown attraction update triggers a catalog refresh',
    build: () {
      when(() => repository.list()).thenAnswer((_) async => seededAttractions);
      return AttractionsBloc(repository: repository);
    },
    act: (bloc) async {
      bloc.add(const AttractionsRequested());
      await pumpEventQueue();
      feed.add(
        AttractionUpdatedMessage(
          AttractionOperationalUpdate(
            eventId: 'evt-new',
            eventType: AttractionUpdateEventType.statusChanged,
            occurredAt: DateTime.parse('2026-08-27T15:00:00Z'),
            attractionId: 'new-ride',
            status: AttractionStatus.operating,
            capacityMode: AttractionCapacityMode.normal,
            waitMinutes: 5,
            statusMessage: null,
            updatedAt: DateTime.parse('2026-08-27T15:00:00Z'),
            version: 1,
          ),
        ),
      );
      await pumpEventQueue();
    },
    expect: () => [
      isA<AttractionsLoading>(),
      isA<AttractionsLoaded>(),
      isA<AttractionsRefreshing>(),
      isA<AttractionsLoaded>(),
    ],
    verify: (_) {
      verify(() => repository.list()).called(2);
    },
  );

  blocTest<AttractionsBloc, AttractionsState>(
    'disconnect retains catalog and marks connection stale',
    build: () {
      when(() => repository.list()).thenAnswer((_) async => seededAttractions);
      return AttractionsBloc(repository: repository);
    },
    act: (bloc) async {
      bloc.add(const AttractionsRequested());
      await pumpEventQueue();
      feed.addError(const NetworkFailure());
      await pumpEventQueue();
    },
    expect: () => [
      isA<AttractionsLoading>(),
      isA<AttractionsLoaded>(),
      isA<AttractionsLoaded>()
          .having((s) => s.attractions, 'attractions', seededAttractions)
          .having(
            (s) => s.connectionStatus,
            'connection',
            LiveConnectionStatus.stale,
          )
          .having(
            (s) => s.connectionMessage,
            'message',
            'Live updates paused—showing last known conditions',
          ),
    ],
  );

  blocTest<AttractionsBloc, AttractionsState>(
    'reconnect snapshot clears stale status',
    build: () {
      when(() => repository.list()).thenAnswer((_) async => seededAttractions);
      return AttractionsBloc(repository: repository);
    },
    act: (bloc) async {
      bloc.add(const AttractionsRequested());
      await pumpEventQueue();
      bloc.add(
        const AttractionsConnectionChanged(
          LiveConnectionStatus.stale,
          message: 'Live updates paused—showing last known conditions',
        ),
      );
      await pumpEventQueue();
      feed.add(AttractionsSnapshotMessage(seededAttractions));
      await pumpEventQueue();
    },
    expect: () => [
      isA<AttractionsLoading>(),
      isA<AttractionsLoaded>(),
      isA<AttractionsLoaded>().having(
        (s) => s.connectionStatus,
        'stale',
        LiveConnectionStatus.stale,
      ),
      isA<AttractionsLoaded>().having(
        (s) => s.connectionStatus,
        'connected',
        LiveConnectionStatus.connected,
      ),
    ],
  );

  test('cancels the live subscription when the bloc closes', () async {
    when(() => repository.list()).thenAnswer((_) async => seededAttractions);
    final bloc = AttractionsBloc(repository: repository);
    bloc.add(const AttractionsRequested());
    await bloc.stream.firstWhere((state) => state is AttractionsLoaded);
    await pumpEventQueue();
    expect(feed.listenCount, 1);
    await bloc.close();
    feed.add(
      AttractionUpdatedMessage(mangroveWaitUpdate(version: 9, waitMinutes: 99)),
    );
    await pumpEventQueue();
    expect(bloc.state, isA<AttractionsLoaded>());
    final loaded = bloc.state as AttractionsLoaded;
    expect(loaded.attractions.first.waitMinutes, 25);
  });
}
