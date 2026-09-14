import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lumen_marsh_app/features/field_guide/data/asset_field_guide_repository.dart';
import 'package:lumen_marsh_app/features/field_guide/domain/field_guide_category.dart';

import '../../../helpers/seeded_field_guide.dart';

class _FakeBundle extends CachingAssetBundle {
  _FakeBundle(this.payloads);

  final Map<String, String> payloads;

  @override
  Future<ByteData> load(String key) async {
    final value = payloads[key];
    if (value == null) {
      throw FlutterError('Unable to load asset: $key');
    }
    final bytes = utf8.encode(value);
    return ByteData.view(Uint8List.fromList(bytes).buffer);
  }

  @override
  Future<String> loadString(String key, {bool cache = true}) async {
    final value = payloads[key];
    if (value == null) {
      throw FlutterError('Unable to load asset: $key');
    }
    return value;
  }
}

void main() {
  test('list parses bundled JSON entries', () async {
    final repository = AssetFieldGuideRepository(
      bundle: _FakeBundle({
        'assets/field_guide/entries.json': seededFieldGuideJson,
      }),
    );

    final entries = await repository.list();

    expect(entries, hasLength(2));
    expect(entries.first.id, 'ghost-orchid');
    expect(entries.first.category, FieldGuideCategory.flora);
    expect(entries.first.bodyParagraphs, ['Paragraph one.', 'Paragraph two.']);
  });

  test('getById returns a matching entry and null for unknown ids', () async {
    final repository = AssetFieldGuideRepository(
      bundle: _FakeBundle({
        'assets/field_guide/entries.json': seededFieldGuideJson,
      }),
    );

    expect(
      (await repository.getById('ghost-orchid'))?.title,
      'Lumen Ghost Orchid',
    );
    expect(await repository.getById('missing'), isNull);
  });

  test('invalid JSON returns an empty catalog', () async {
    final repository = AssetFieldGuideRepository(
      bundle: _FakeBundle({'assets/field_guide/entries.json': '{not-json'}),
    );

    expect(await repository.list(), isEmpty);
  });

  test('missing asset returns an empty catalog', () async {
    final repository = AssetFieldGuideRepository(bundle: _FakeBundle({}));

    expect(await repository.list(), isEmpty);
  });
}
