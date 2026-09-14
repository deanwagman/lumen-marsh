class SseEvent {
  const SseEvent({this.id, this.event, this.retry, required this.data});

  final String? id;
  final String? event;
  final Duration? retry;
  final String data;
}

/// Accumulates SSE wire lines into complete events at blank-line boundaries.
///
/// Chunks may split mid-line; incomplete trailing text is buffered until the
/// next chunk provides a newline.
class SseEventParser {
  final StringBuffer _data = StringBuffer();
  String? _id;
  String? _event;
  Duration? _retry;
  bool _hasData = false;
  String _carry = '';

  List<SseEvent> addChunk(String chunk) {
    final events = <SseEvent>[];
    final text = '$_carry$chunk';
    var start = 0;
    for (var i = 0; i < text.length; i++) {
      if (text.codeUnitAt(i) != 0x0A) {
        continue;
      }
      var line = text.substring(start, i);
      if (line.endsWith('\r')) {
        line = line.substring(0, line.length - 1);
      }
      final completed = _addLine(line);
      if (completed != null) {
        events.add(completed);
      }
      start = i + 1;
    }
    _carry = text.substring(start);
    return events;
  }

  SseEvent? _addLine(String line) {
    if (line.isEmpty) {
      if (!_hasData && _id == null && _event == null && _retry == null) {
        return null;
      }
      final event = SseEvent(
        id: _id,
        event: _event,
        retry: _retry,
        data: _data.toString(),
      );
      _reset();
      return event;
    }

    if (line.startsWith(':')) {
      return null;
    }

    final separator = line.indexOf(':');
    final field = separator == -1 ? line : line.substring(0, separator);
    var value = separator == -1 ? '' : line.substring(separator + 1);
    if (value.startsWith(' ')) {
      value = value.substring(1);
    }

    switch (field) {
      case 'event':
        _event = value;
      case 'data':
        if (_hasData) {
          _data.write('\n');
        }
        _data.write(value);
        _hasData = true;
      case 'id':
        _id = value;
      case 'retry':
        final millis = int.tryParse(value);
        if (millis != null) {
          _retry = Duration(milliseconds: millis);
        }
    }
    return null;
  }

  void _reset() {
    _data.clear();
    _id = null;
    _event = null;
    _retry = null;
    _hasData = false;
  }
}
