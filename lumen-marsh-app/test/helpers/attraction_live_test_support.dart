import 'dart:async';

import 'package:mocktail/mocktail.dart';
import 'package:lumen_marsh_app/features/attractions/data/attraction_repository.dart';
import 'package:lumen_marsh_app/features/attractions/domain/attraction_stream_message.dart';

/// Idle live stream so AttractionsBloc can subscribe without opening SSE.
void stubIdleAttractionLiveStream(AttractionRepository repository) {
  when(() => repository.watchUpdates()).thenAnswer((_) {
    return StreamController<AttractionStreamMessage>.broadcast().stream;
  });
  when(() => repository.dispose()).thenAnswer((_) async {});
}

/// Controllable broadcast stream for live-update tests.
class FakeAttractionLiveFeed {
  FakeAttractionLiveFeed() : _controller = StreamController.broadcast();

  final StreamController<AttractionStreamMessage> _controller;
  var listenCount = 0;
  var closed = false;

  Stream<AttractionStreamMessage> open() {
    listenCount += 1;
    return _controller.stream;
  }

  void add(AttractionStreamMessage message) => _controller.add(message);

  void addError(Object error, [StackTrace? stackTrace]) {
    _controller.addError(error, stackTrace);
  }

  Future<void> close() async {
    closed = true;
    await _controller.close();
  }
}

void stubAttractionLiveFeed(
  AttractionRepository repository,
  FakeAttractionLiveFeed feed,
) {
  when(() => repository.watchUpdates()).thenAnswer((_) => feed.open());
  when(() => repository.dispose()).thenAnswer((_) async {
    await feed.close();
  });
}
