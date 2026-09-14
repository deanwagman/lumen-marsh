import 'dart:async';
import 'dart:convert';
import 'dart:math';

import 'package:http/http.dart' as http;

import '../../../features/attractions/data/sse_event_parser.dart';
import '../domain/venue_stream_message.dart';
import 'venue_event_source.dart';
import 'venue_stream_codec.dart';

class HttpVenueEventSource implements VenueEventSource {
  HttpVenueEventSource({
    required this.baseUrl,
    http.Client? client,
    this.codec = const VenueStreamCodec(),
    this.maxBackoff = const Duration(seconds: 30),
  }) : _client = client ?? http.Client(),
       _ownsClient = client == null;

  final String baseUrl;
  final http.Client _client;
  final VenueStreamCodec codec;
  final Duration maxBackoff;
  final bool _ownsClient;

  StreamController<VenueStreamMessage>? _controller;
  StreamSubscription<List<int>>? _subscription;
  var _closed = false;
  var _attempt = 0;

  Uri get _eventsUri => Uri.parse('$baseUrl/api/v1/events');

  @override
  Stream<VenueStreamMessage> connect() {
    _controller ??= StreamController<VenueStreamMessage>.broadcast(
      onListen: _ensureConnected,
      onCancel: () async {
        if (_controller?.hasListener == false) {
          await _tearDownConnection();
        }
      },
    );
    return _controller!.stream;
  }

  Future<void> _ensureConnected() async {
    if (_closed || _subscription != null) {
      return;
    }
    await _open();
  }

  Future<void> _open() async {
    if (_closed) {
      return;
    }

    try {
      final request = http.Request('GET', _eventsUri);
      request.headers['Accept'] = 'text/event-stream';
      request.headers['Cache-Control'] = 'no-cache';
      final response = await _client.send(request);
      if (response.statusCode < 200 || response.statusCode >= 300) {
        throw http.ClientException(
          'SSE connection failed (${response.statusCode})',
          _eventsUri,
        );
      }

      _attempt = 0;
      final parser = SseEventParser();
      _subscription = response.stream.listen(
        (chunk) {
          final text = utf8.decode(chunk, allowMalformed: true);
          for (final event in parser.addChunk(text)) {
            if (event.data.isEmpty && event.event == null) {
              continue;
            }
            try {
              final message = codec.decodeEvent(
                event: event.event,
                data: event.data,
              );
              if (message != null && message is! VenueStreamUnknownMessage) {
                _controller?.add(message);
              }
            } catch (error, stackTrace) {
              _controller?.addError(error, stackTrace);
            }
          }
        },
        onError: (Object error, StackTrace stackTrace) {
          _controller?.addError(error, stackTrace);
          _scheduleReconnect();
        },
        onDone: _scheduleReconnect,
        cancelOnError: false,
      );
    } catch (error, stackTrace) {
      _controller?.addError(error, stackTrace);
      await _scheduleReconnect();
    }
  }

  Future<void> _scheduleReconnect() async {
    await _tearDownConnection(keepController: true);
    if (_closed || _controller == null || !_controller!.hasListener) {
      return;
    }

    _attempt += 1;
    final exponential = min(
      maxBackoff.inMilliseconds,
      1000 * pow(2, _attempt - 1).toInt(),
    );
    final jitter = Random().nextInt(250);
    await Future<void>.delayed(Duration(milliseconds: exponential + jitter));
    if (!_closed && _controller?.hasListener == true) {
      await _open();
    }
  }

  Future<void> _tearDownConnection({bool keepController = false}) async {
    await _subscription?.cancel();
    _subscription = null;
    if (!keepController) {
      // no-op placeholder for symmetry
    }
  }

  @override
  Future<void> close() async {
    _closed = true;
    await _tearDownConnection();
    await _controller?.close();
    _controller = null;
    if (_ownsClient) {
      _client.close();
    }
  }
}
