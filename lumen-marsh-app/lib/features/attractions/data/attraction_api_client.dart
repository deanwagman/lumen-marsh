import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../../core/errors/app_failure.dart';
import '../domain/attraction_detail.dart';
import '../domain/attraction_summary.dart';

class AttractionApiClient {
  AttractionApiClient({
    required this.client,
    required this.baseUrl,
    this.timeout = const Duration(seconds: 10),
  });

  final http.Client client;
  final String baseUrl;
  final Duration timeout;

  Future<List<AttractionSummary>> fetchAll() async {
    final body = await _getJson('/api/v1/attractions');
    if (body is! List) {
      throw const UnexpectedFailure('Unexpected attractions payload.');
    }
    try {
      return body
          .map(
            (item) => AttractionSummary.fromJson(
              Map<String, dynamic>.from(item as Map),
            ),
          )
          .toList();
    } on FormatException {
      throw const UnexpectedFailure('Could not read the attractions catalog.');
    }
  }

  Future<AttractionDetail> fetchById(String id) async {
    final body = await _getJson('/api/v1/attractions/$id');
    if (body is! Map) {
      throw const UnexpectedFailure('Unexpected attraction payload.');
    }
    try {
      return AttractionDetail.fromJson(Map<String, dynamic>.from(body));
    } on FormatException {
      throw const UnexpectedFailure('Could not read that attraction.');
    }
  }

  Future<dynamic> _getJson(String path) async {
    final uri = Uri.parse('$baseUrl$path');
    late final http.Response response;
    try {
      response = await client
          .get(uri, headers: const {'Accept': 'application/json'})
          .timeout(timeout);
    } on TimeoutException {
      throw const NetworkFailure();
    } on http.ClientException {
      throw const NetworkFailure();
    } catch (error) {
      if (_isNetworkError(error)) {
        throw const NetworkFailure();
      }
      throw const UnexpectedFailure();
    }

    if (response.statusCode == 404) {
      throw const NotFoundFailure();
    }
    if (response.statusCode >= 500) {
      throw const ServerFailure();
    }
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw ServerFailure(
        'VenueOps returned an unexpected status (${response.statusCode}).',
      );
    }

    try {
      return jsonDecode(response.body);
    } on FormatException {
      throw const UnexpectedFailure(
        'VenueOps returned an unreadable response.',
      );
    }
  }
}

bool _isNetworkError(Object error) {
  final name = error.runtimeType.toString();
  return name == 'SocketException' ||
      name == 'HandshakeException' ||
      name == 'OSError';
}
