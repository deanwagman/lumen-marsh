import '../domain/venue_stream_message.dart';

abstract interface class VenueEventSource {
  Stream<VenueStreamMessage> connect();

  Future<void> close();
}
