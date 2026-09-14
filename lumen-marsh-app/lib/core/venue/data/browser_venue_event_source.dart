import 'dart:async';
import 'dart:js_interop';

import 'package:web/web.dart' as web;

import '../domain/venue_stream_message.dart';
import 'venue_event_source.dart';
import 'venue_stream_codec.dart';

class BrowserVenueEventSource implements VenueEventSource {
  BrowserVenueEventSource({
    required this.baseUrl,
    this.codec = const VenueStreamCodec(),
  });

  final String baseUrl;
  final VenueStreamCodec codec;

  StreamController<VenueStreamMessage>? _controller;
  web.EventSource? _source;
  var _closed = false;

  String get _url => '$baseUrl/api/v1/events';

  static const _events = [
    'attractions.snapshot',
    'attraction.updated',
    'advisories.snapshot',
    'advisory.published',
    'advisory.updated',
    'advisory.withdrawn',
  ];

  @override
  Stream<VenueStreamMessage> connect() {
    _controller ??= StreamController<VenueStreamMessage>.broadcast(
      onListen: _ensureConnected,
      onCancel: () async {
        if (_controller?.hasListener == false) {
          await _disconnect();
        }
      },
    );
    return _controller!.stream;
  }

  void _ensureConnected() {
    if (_closed || _source != null) {
      return;
    }
    final source = web.EventSource(_url);
    _source = source;

    for (final name in _events) {
      source.addEventListener(name, _onEvent.toJS);
    }
    source.onerror = ((web.Event _) {
      if (source.readyState == web.EventSource.CLOSED) {
        _controller?.addError(StateError('Venue event stream closed'));
      }
    }).toJS;
  }

  void _onEvent(web.Event raw) {
    final event = raw as web.MessageEvent;
    final data = event.data?.dartify()?.toString() ?? '';
    try {
      final message = codec.decodeEvent(event: event.type, data: data);
      if (message != null && message is! VenueStreamUnknownMessage) {
        _controller?.add(message);
      }
    } catch (error, stackTrace) {
      _controller?.addError(error, stackTrace);
    }
  }

  Future<void> _disconnect() async {
    _source?.close();
    _source = null;
  }

  @override
  Future<void> close() async {
    _closed = true;
    await _disconnect();
    await _controller?.close();
    _controller = null;
  }
}
