import 'dart:async';
import 'dart:js_interop';

import 'package:web/web.dart' as web;

import '../domain/attraction_stream_message.dart';
import 'attraction_event_source.dart';
import 'attraction_stream_codec.dart';

class BrowserAttractionEventSource implements AttractionEventSource {
  BrowserAttractionEventSource({
    required this.baseUrl,
    this.codec = const AttractionStreamCodec(),
  });

  final String baseUrl;
  final AttractionStreamCodec codec;

  StreamController<AttractionStreamMessage>? _controller;
  web.EventSource? _source;
  var _closed = false;

  String get _url => '$baseUrl/api/v1/attractions/events';

  @override
  Stream<AttractionStreamMessage> connect() {
    _controller ??= StreamController<AttractionStreamMessage>.broadcast(
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

    source.addEventListener('attractions.snapshot', _onEvent.toJS);
    source.addEventListener('attraction.updated', _onEvent.toJS);
    source.onerror = ((web.Event _) {
      if (source.readyState == web.EventSource.CLOSED) {
        _controller?.addError(StateError('Attraction event stream closed'));
      }
    }).toJS;
  }

  void _onEvent(web.Event raw) {
    final event = raw as web.MessageEvent;
    final data = event.data?.dartify()?.toString() ?? '';
    try {
      final message = codec.decodeEvent(event: event.type, data: data);
      _controller?.add(message);
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
