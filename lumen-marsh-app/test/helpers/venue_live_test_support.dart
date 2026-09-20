import 'dart:async';

import 'package:mocktail/mocktail.dart';
import 'package:lumen_marsh_app/core/venue/data/venue_event_hub.dart';
import 'package:lumen_marsh_app/core/venue/domain/venue_live.dart';
import 'package:lumen_marsh_app/core/venue/domain/venue_stream_message.dart';
import 'package:lumen_marsh_app/features/advisories/data/advisory_api_client.dart';
import 'package:lumen_marsh_app/features/advisories/data/advisory_repository.dart';
import 'package:lumen_marsh_app/features/advisories/presentation/bloc/advisories_bloc.dart';
import 'package:lumen_marsh_app/features/advisories/presentation/bloc/advisories_state.dart';
import 'package:lumen_marsh_app/features/attractions/data/attraction_repository.dart';
import 'package:lumen_marsh_app/features/flow/data/flow_repository.dart';
import 'package:lumen_marsh_app/features/flow/domain/guest_wait.dart';

/// Idle live stream so blocs can subscribe without opening SSE.
void stubIdleVenueLiveStream(VenueEventHub hub) {
  when(() => hub.watchAll()).thenAnswer((_) {
    return StreamController<VenueStreamMessage>.broadcast().stream;
  });
  when(() => hub.watchAttractions()).thenAnswer((_) {
    return StreamController<VenueStreamMessage>.broadcast().stream;
  });
  when(() => hub.watchAdvisories()).thenAnswer((_) {
    return StreamController<VenueStreamMessage>.broadcast().stream;
  });
  when(() => hub.watchFlow()).thenAnswer((_) {
    return StreamController<VenueStreamMessage>.broadcast().stream;
  });
  when(() => hub.watchConnectionStatus())
      .thenAnswer((_) => const Stream<LiveConnectionStatus>.empty());
  when(() => hub.dispose()).thenAnswer((_) async {});
}

void stubIdleAttractionLiveStream(AttractionRepository repository) {
  when(() => repository.watchUpdates()).thenAnswer((_) {
    return StreamController<VenueStreamMessage>.broadcast().stream;
  });
  when(() => repository.dispose()).thenAnswer((_) async {});
}

void stubIdleAdvisoryLiveStream(AdvisoryRepository repository) {
  when(() => repository.watchUpdates()).thenAnswer((_) {
    return StreamController<VenueStreamMessage>.broadcast().stream;
  });
  when(() => repository.watchConnectionStatus())
      .thenAnswer((_) => const Stream<LiveConnectionStatus>.empty());
  when(() => repository.dispose()).thenAnswer((_) async {});
}

void stubIdleFlowLiveStream(FlowRepository repository) {
  when(() => repository.watchUpdates()).thenAnswer((_) {
    return StreamController<VenueStreamMessage>.broadcast().stream;
  });
  when(() => repository.watchConnectionStatus())
      .thenAnswer((_) => const Stream<LiveConnectionStatus>.empty());
  when(() => repository.dispose()).thenAnswer((_) async {});
}

final emptyGuestFlowOverview = GuestFlowOverview(
  attractions: const [],
  publishedGuidance: const [],
  updatedAt: DateTime.parse('2026-08-27T14:30:00Z'),
  simulated: false,
);

void stubEmptyFlow(FlowRepository repository) {
  when(() => repository.overview())
      .thenAnswer((_) async => emptyGuestFlowOverview);
  stubIdleFlowLiveStream(repository);
}

class MockAdvisoriesBloc extends Mock implements AdvisoriesBloc {}

void stubEmptyAdvisories(MockAdvisoriesBloc bloc) {
  when(() => bloc.state)
      .thenReturn(AdvisoriesEmpty(lastSyncedAt: DateTime.utc(2026, 8, 27, 15)));
  when(() => bloc.stream).thenAnswer(
    (_) => Stream.value(
      AdvisoriesEmpty(lastSyncedAt: DateTime.utc(2026, 8, 27, 15)),
    ),
  );
}

class FakeVenueLiveFeed {
  FakeVenueLiveFeed() : _controller = StreamController.broadcast();

  final StreamController<VenueStreamMessage> _controller;
  var listenCount = 0;
  var closed = false;

  Stream<VenueStreamMessage> open() {
    listenCount += 1;
    return _controller.stream;
  }

  void add(VenueStreamMessage message) => _controller.add(message);

  void addError(Object error, [StackTrace? stackTrace]) {
    _controller.addError(error, stackTrace);
  }

  Future<void> close() async {
    closed = true;
    await _controller.close();
  }
}

void stubVenueLiveFeed(VenueEventHub hub, FakeVenueLiveFeed feed) {
  when(() => hub.watchAll()).thenAnswer((_) => feed.open());
  when(() => hub.watchAttractions()).thenAnswer((_) => feed.open());
  when(() => hub.watchAdvisories()).thenAnswer((_) => feed.open());
  when(() => hub.watchFlow()).thenAnswer((_) => feed.open());
  when(() => hub.watchConnectionStatus())
      .thenAnswer((_) => const Stream<LiveConnectionStatus>.empty());
  when(() => hub.dispose()).thenAnswer((_) async {
    await feed.close();
  });
}

void stubAttractionLiveFeed(
  AttractionRepository repository,
  FakeVenueLiveFeed feed,
) {
  when(() => repository.watchUpdates()).thenAnswer((_) => feed.open());
  when(() => repository.dispose()).thenAnswer((_) async {});
}

void stubAdvisoryLiveFeed(
  AdvisoryRepository repository,
  FakeVenueLiveFeed feed,
) {
  when(() => repository.watchUpdates()).thenAnswer((_) => feed.open());
  when(() => repository.watchConnectionStatus())
      .thenAnswer((_) => const Stream<LiveConnectionStatus>.empty());
  when(() => repository.dispose()).thenAnswer((_) async {});
}

HttpAdvisoryRepository createTestAdvisoryRepository({
  required AdvisoryApiClient apiClient,
  required VenueEventHub hub,
}) {
  return HttpAdvisoryRepository(apiClient, hub);
}
