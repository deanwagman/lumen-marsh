import 'dart:async';

import '../../../core/errors/app_failure.dart';
import '../domain/venue_live.dart';
import '../domain/venue_stream_message.dart';
import 'venue_event_source.dart';
import 'venue_event_source_factory.dart';

/// Owns one park-wide SSE connection and exposes filtered streams to features.
class VenueEventHub {
  VenueEventHub({required String baseUrl, VenueEventSource? eventSource})
    : _eventSource = eventSource ?? createVenueEventSource(baseUrl: baseUrl);

  final VenueEventSource _eventSource;
  Stream<VenueStreamMessage>? _broadcast;
  final _connectionController =
      StreamController<LiveConnectionStatus>.broadcast();

  Stream<VenueStreamMessage> watchAll() {
    return _broadcast ??= _eventSource.connect().transform(
      StreamTransformer<VenueStreamMessage, VenueStreamMessage>.fromHandlers(
        handleData: (data, sink) {
          _connectionController.add(LiveConnectionStatus.connected);
          sink.add(data);
        },
        handleError: (error, stackTrace, sink) {
          _connectionController.add(LiveConnectionStatus.stale);
          sink.addError(_toFailure(error), stackTrace);
        },
      ),
    );
  }

  Stream<VenueStreamMessage> watchAttractions() {
    return watchAll().where(
      (message) =>
          message is AttractionsSnapshotMessage ||
          message is AttractionUpdatedMessage,
    );
  }

  Stream<VenueStreamMessage> watchAdvisories() {
    return watchAll().where(
      (message) =>
          message is AdvisoriesSnapshotMessage ||
          message is AdvisoryPublishedMessage ||
          message is AdvisoryUpdatedMessage ||
          message is AdvisoryWithdrawnMessage,
    );
  }

  Stream<VenueStreamMessage> watchFlow() {
    return watchAll().where(
      (message) =>
          message is GuestFlowOverviewMessage ||
          message is GuestFlowWaitUpdatedMessage ||
          message is GuestFlowRecommendationPublishedMessage ||
          message is GuestFlowRecommendationWithdrawnMessage,
    );
  }

  Stream<LiveConnectionStatus> watchConnectionStatus() =>
      _connectionController.stream;

  void markConnecting() {
    _connectionController.add(LiveConnectionStatus.connecting);
  }

  void markReconnecting() {
    _connectionController.add(LiveConnectionStatus.reconnecting);
  }

  Future<void> dispose() async {
    await _eventSource.close();
    _broadcast = null;
    await _connectionController.close();
  }

  AppFailure _toFailure(Object error) {
    if (error is AppFailure) {
      return error;
    }
    if (error is FormatException) {
      return UnexpectedFailure(error.message);
    }
    return const NetworkFailure();
  }
}
