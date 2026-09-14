import 'dart:async';

import '../../../core/venue/data/venue_event_hub.dart';
import '../../../core/venue/domain/venue_live.dart';
import '../../../core/venue/domain/venue_stream_message.dart';
import '../domain/guest_advisory.dart';
import 'advisory_api_client.dart';

abstract interface class AdvisoryRepository {
  Future<List<GuestAdvisory>> listActive();

  Stream<VenueStreamMessage> watchUpdates();

  Stream<LiveConnectionStatus> watchConnectionStatus();

  Future<void> dispose();
}

class HttpAdvisoryRepository implements AdvisoryRepository {
  HttpAdvisoryRepository(this._apiClient, this._eventHub);

  final AdvisoryApiClient _apiClient;
  final VenueEventHub _eventHub;
  Stream<VenueStreamMessage>? _broadcast;

  @override
  Future<List<GuestAdvisory>> listActive() => _apiClient.fetchActive();

  @override
  Stream<VenueStreamMessage> watchUpdates() {
    return _broadcast ??= _eventHub.watchAdvisories();
  }

  @override
  Stream<LiveConnectionStatus> watchConnectionStatus() =>
      _eventHub.watchConnectionStatus();

  @override
  Future<void> dispose() async {
    _broadcast = null;
  }
}
