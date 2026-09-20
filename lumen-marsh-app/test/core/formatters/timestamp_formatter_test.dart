import 'package:flutter_test/flutter_test.dart';
import 'package:lumen_marsh_app/core/formatters/timestamp_formatter.dart';

void main() {
  test('formatWaitAge uses minute-scale language for recent updates', () {
    final now = DateTime.utc(2026, 9, 15, 18, 32);
    expect(
      formatWaitAge(DateTime.utc(2026, 9, 15, 18, 31), now: now),
      '1 minute ago',
    );
    expect(
      formatWaitAge(DateTime.utc(2026, 9, 15, 18, 20), now: now),
      '12 minutes ago',
    );
    expect(
      formatWaitAge(DateTime.utc(2026, 9, 15, 18, 31, 50), now: now),
      'just now',
    );
  });
}
