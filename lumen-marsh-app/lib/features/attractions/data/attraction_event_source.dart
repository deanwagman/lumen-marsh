import '../domain/attraction_stream_message.dart';

abstract interface class AttractionEventSource {
  Stream<AttractionStreamMessage> connect();

  Future<void> close();
}
