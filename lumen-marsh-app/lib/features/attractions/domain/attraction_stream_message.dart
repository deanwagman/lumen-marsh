import '../../../core/venue/domain/venue_stream_message.dart';
export '../../../core/venue/domain/venue_stream_message.dart'
    show
        AttractionsSnapshotMessage,
        AttractionUpdatedMessage,
        VenueStreamMessage;
export '../../../core/venue/domain/venue_live.dart' show LiveConnectionStatus;

typedef AttractionStreamMessage = VenueStreamMessage;
