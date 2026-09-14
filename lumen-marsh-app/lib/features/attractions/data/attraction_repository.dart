import 'dart:async';

import '../../../core/errors/app_failure.dart';
import '../../../core/venue/data/venue_event_hub.dart';
import '../../../core/venue/domain/venue_stream_message.dart';
import '../domain/attraction_detail.dart';
import '../domain/attraction_summary.dart';
import 'attraction_api_client.dart';

abstract interface class AttractionRepository {
  Future<List<AttractionSummary>> list();

  Future<AttractionDetail> getById(String id);

  Stream<VenueStreamMessage> watchUpdates();

  Future<void> dispose();
}

class HttpAttractionRepository implements AttractionRepository {
  HttpAttractionRepository(this._apiClient, this._eventHub);

  final AttractionApiClient _apiClient;
  final VenueEventHub _eventHub;
  Stream<VenueStreamMessage>? _broadcast;

  @override
  Future<List<AttractionSummary>> list() => _apiClient.fetchAll();

  @override
  Future<AttractionDetail> getById(String id) => _apiClient.fetchById(id);

  @override
  Stream<VenueStreamMessage> watchUpdates() {
    return _broadcast ??= _eventHub.watchAttractions().transform(
      StreamTransformer<VenueStreamMessage, VenueStreamMessage>.fromHandlers(
        handleError: (error, stackTrace, sink) {
          sink.addError(_toFailure(error), stackTrace);
        },
      ),
    );
  }

  @override
  Future<void> dispose() async {
    _broadcast = null;
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
