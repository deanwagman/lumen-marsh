import 'package:flutter_test/flutter_test.dart';
import 'package:lumen_marsh_app/features/attractions/data/media_url_resolver.dart';

void main() {
  test('resolves relative media paths against the API base URL', () {
    expect(
      resolveMediaUrl(
        'http://localhost:8080',
        '/media/attractions/mangrove-run/hero.webp',
      ),
      'http://localhost:8080/media/attractions/mangrove-run/hero.webp',
    );
  });

  test('preserves absolute media URLs', () {
    expect(
      resolveMediaUrl(
        'http://localhost:8080',
        'https://cdn.example.com/hero.webp',
      ),
      'https://cdn.example.com/hero.webp',
    );
  });
}
