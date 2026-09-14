import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

import 'app/app.dart';
import 'app/config/app_config.dart';
import 'core/venue/data/venue_event_hub.dart';
import 'features/advisories/data/advisory_api_client.dart';
import 'features/advisories/data/advisory_repository.dart';
import 'features/attractions/data/attraction_api_client.dart';
import 'features/attractions/data/attraction_repository.dart';
import 'features/favorites/data/shared_preferences_favorites_repository.dart';
import 'features/field_guide/data/asset_field_guide_repository.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final config = AppConfig.fromEnvironment();
  final preferences = await SharedPreferences.getInstance();
  final eventHub = VenueEventHub(baseUrl: config.apiBaseUrl);
  final apiClient = AttractionApiClient(
    client: http.Client(),
    baseUrl: config.apiBaseUrl,
  );
  final attractionRepository = HttpAttractionRepository(apiClient, eventHub);
  final advisoryRepository = HttpAdvisoryRepository(
    AdvisoryApiClient(client: http.Client(), baseUrl: config.apiBaseUrl),
    eventHub,
  );
  final favoritesRepository = SharedPreferencesFavoritesRepository(
    preferences: preferences,
  );
  final fieldGuideRepository = AssetFieldGuideRepository();

  runApp(
    LumenMarshApp(
      config: config,
      eventHub: eventHub,
      attractionRepository: attractionRepository,
      advisoryRepository: advisoryRepository,
      favoritesRepository: favoritesRepository,
      fieldGuideRepository: fieldGuideRepository,
    ),
  );
}
