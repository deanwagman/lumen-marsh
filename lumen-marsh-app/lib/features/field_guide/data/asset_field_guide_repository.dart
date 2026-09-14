import 'dart:convert';

import 'package:flutter/services.dart';

import '../domain/field_guide_entry.dart';
import '../domain/field_guide_repository.dart';

class AssetFieldGuideRepository implements FieldGuideRepository {
  AssetFieldGuideRepository({
    AssetBundle? bundle,
    this.assetPath = 'assets/field_guide/entries.json',
  }) : _bundle = bundle ?? rootBundle;

  final AssetBundle _bundle;
  final String assetPath;

  List<FieldGuideEntry>? _cache;

  @override
  Future<List<FieldGuideEntry>> list() async {
    return List.unmodifiable(await _load());
  }

  @override
  Future<FieldGuideEntry?> getById(String id) async {
    for (final entry in await _load()) {
      if (entry.id == id) {
        return entry;
      }
    }
    return null;
  }

  Future<List<FieldGuideEntry>> _load() async {
    if (_cache != null) {
      return _cache!;
    }

    try {
      final raw = await _bundle.loadString(assetPath);
      final decoded = jsonDecode(raw);
      if (decoded is! List) {
        _cache = const [];
        return _cache!;
      }

      _cache = [
        for (final item in decoded)
          if (item is Map<String, dynamic>)
            FieldGuideEntry.fromJson(item)
          else if (item is Map)
            FieldGuideEntry.fromJson(Map<String, dynamic>.from(item)),
      ];
      return _cache!;
    } catch (_) {
      _cache = const [];
      return _cache!;
    }
  }
}
