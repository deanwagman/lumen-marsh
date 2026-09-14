import 'venue_event_source.dart';
import 'http_venue_event_source.dart';

VenueEventSource createPlatformVenueEventSource({required String baseUrl}) {
  return HttpVenueEventSource(baseUrl: baseUrl);
}
