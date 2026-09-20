import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../../../core/errors/app_failure.dart';
import '../domain/guest_wait.dart';

class FlowApiClient {
  FlowApiClient({
    required this.client,
    required this.baseUrl,
    this.timeout = const Duration(seconds: 10),
  });

  final http.Client client;
  final String baseUrl;
  final Duration timeout;

  Future<GuestFlowOverview> fetchOverview() async {
    final body = await _getJson('/api/v1/flow/overview');
    if (body is! Map) {
      throw const UnexpectedFailure('Unexpected park flow payload.');
    }
    try {
      return GuestFlowOverview.fromJson(Map<String, dynamic>.from(body));
    } on FormatException {
      throw const UnexpectedFailure('Could not read park flow.');
    }
  }

  Future<List<GuestGuidance>> fetchRecommendations() async {
    final body = await _getJson('/api/v1/flow/recommendations');
    if (body is! List) {
      throw const UnexpectedFailure('Unexpected flow recommendations payload.');
    }
    try {
      return [
        for (final item in body)
          GuestGuidance.fromJson(Map<String, dynamic>.from(item as Map)),
      ];
    } on FormatException {
      throw const UnexpectedFailure('Could not read flow recommendations.');
    }
  }

  Future<GuestWait> fetchWaitForecast(String attractionId) async {
    final body = await _getJson(
      '/api/v1/attractions/$attractionId/wait-forecast',
    );
    if (body is! Map) {
      throw const UnexpectedFailure('Unexpected wait forecast payload.');
    }
    try {
      return GuestWait.fromJson(Map<String, dynamic>.from(body));
    } on FormatException {
      throw const UnexpectedFailure('Could not read that wait forecast.');
    }
  }

  Future<Object?> _getJson(String path) async {
    final uri = Uri.parse('$baseUrl$path');
    try {
      final response = await client
          .get(uri, headers: const {'Accept': 'application/json'})
          .timeout(timeout);
      if (response.statusCode == 404) {
        throw const NotFoundFailure();
      }
      if (response.statusCode < 200 || response.statusCode >= 300) {
        throw const ServerFailure();
      }
      return jsonDecode(response.body);
    } on AppFailure {
      rethrow;
    } on TimeoutException {
      throw const NetworkFailure();
    } on FormatException {
      throw const UnexpectedFailure();
    } catch (_) {
      throw const NetworkFailure();
    }
  }
}
