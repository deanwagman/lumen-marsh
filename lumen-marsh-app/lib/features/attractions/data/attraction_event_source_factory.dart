import 'attraction_event_source.dart';
import 'create_event_source_stub.dart'
    if (dart.library.html) 'create_event_source_web.dart'
    if (dart.library.io) 'create_event_source_io.dart';

AttractionEventSource createAttractionEventSource({required String baseUrl}) {
  return createPlatformAttractionEventSource(baseUrl: baseUrl);
}
