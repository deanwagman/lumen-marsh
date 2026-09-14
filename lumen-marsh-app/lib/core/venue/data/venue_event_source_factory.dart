import 'venue_event_source.dart';
import 'create_venue_event_source_stub.dart'
    if (dart.library.html) 'create_venue_event_source_web.dart'
    if (dart.library.io) 'create_venue_event_source_io.dart';

VenueEventSource createVenueEventSource({required String baseUrl}) {
  return createPlatformVenueEventSource(baseUrl: baseUrl);
}
