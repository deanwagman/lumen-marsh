import 'attraction_event_source.dart';
import 'http_attraction_event_source.dart';

AttractionEventSource createPlatformAttractionEventSource({
  required String baseUrl,
}) {
  return HttpAttractionEventSource(baseUrl: baseUrl);
}
