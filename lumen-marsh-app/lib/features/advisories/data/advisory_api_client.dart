import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../../core/errors/app_failure.dart';
import '../domain/guest_advisory.dart';

class AdvisoryApiClient {
  AdvisoryApiClient({
    required this.client,
    required this.baseUrl,
    this.timeout = const Duration(seconds: 10),
  });

  final http.Client client;
  final String baseUrl;
  final Duration timeout;

  Future<List<GuestAdvisory>> fetchActive() async {
    final body = await _getJson('/api/v1/advisories');
    if (body is! List) {
      throw const UnexpectedFailure('Unexpected advisories payload.');
    }
    try {
      return body
          .map(
            (item) =>
                GuestAdvisory.fromJson(Map<String, dynamic>.from(item as Map)),
          )
          .toList();
    } on FormatException {
      throw const UnexpectedFailure('Could not read active advisories.');
    }
  }

  Future<Object?> _getJson(String path) async {
    final uri = Uri.parse('$baseUrl$path');
    try {
      final response = await client.get(uri).timeout(timeout);
      if (response.statusCode == 404) {
        throw const NotFoundFailure();
      }
      if (response.statusCode < 200 || response.statusCode >= 300) {
        throw const ServerFailure();
      }
      return jsonDecode(response.body);
    } on AppFailure {
      rethrow;
    } on FormatException {
      throw const UnexpectedFailure();
    } catch (_) {
      throw const NetworkFailure();
    }
  }
}
