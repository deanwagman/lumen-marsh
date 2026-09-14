import 'attraction_event_source.dart';
import 'browser_attraction_event_source.dart';

AttractionEventSource createPlatformAttractionEventSource({
  required String baseUrl,
}) {
  return BrowserAttractionEventSource(baseUrl: baseUrl);
}
