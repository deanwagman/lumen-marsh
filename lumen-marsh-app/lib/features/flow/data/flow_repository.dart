import 'dart:async';

import '../../../core/venue/data/venue_event_hub.dart';
import '../../../core/venue/domain/venue_live.dart';
import '../../../core/venue/domain/venue_stream_message.dart';
import '../domain/guest_wait.dart';
import 'flow_api_client.dart';

abstract interface class FlowRepository {
  Future<GuestFlowOverview> overview();

  Future<GuestWait> waitForecast(String attractionId);

  Stream<VenueStreamMessage> watchUpdates();

  Stream<LiveConnectionStatus> watchConnectionStatus();

  Future<void> dispose();
}

class HttpFlowRepository implements FlowRepository {
  HttpFlowRepository(this._apiClient, this._eventHub);

  final FlowApiClient _apiClient;
  final VenueEventHub _eventHub;
  Stream<VenueStreamMessage>? _broadcast;

  @override
  Future<GuestFlowOverview> overview() => _apiClient.fetchOverview();

  @override
  Future<GuestWait> waitForecast(String attractionId) =>
      _apiClient.fetchWaitForecast(attractionId);

  @override
  Stream<VenueStreamMessage> watchUpdates() {
    return _broadcast ??= _eventHub.watchFlow();
  }

  @override
  Stream<LiveConnectionStatus> watchConnectionStatus() =>
      _eventHub.watchConnectionStatus();

  @override
  Future<void> dispose() async {
    _broadcast = null;
  }
}
