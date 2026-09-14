import 'attraction_event_source.dart';

AttractionEventSource createPlatformAttractionEventSource({
  required String baseUrl,
}) {
  throw UnsupportedError('Attraction SSE is not supported on this platform.');
}
