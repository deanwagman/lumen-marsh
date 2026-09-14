import 'venue_event_source.dart';
import 'browser_venue_event_source.dart';

VenueEventSource createPlatformVenueEventSource({required String baseUrl}) {
  return BrowserVenueEventSource(baseUrl: baseUrl);
}
