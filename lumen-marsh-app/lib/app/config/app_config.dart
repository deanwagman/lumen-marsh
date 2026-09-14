class AppConfig {
  const AppConfig({required this.apiBaseUrl});

  factory AppConfig.fromEnvironment() {
    const raw = String.fromEnvironment(
      'VENUEOPS_API_BASE_URL',
      defaultValue: 'http://localhost:8080',
    );
    return AppConfig(apiBaseUrl: _trimTrailingSlash(raw));
  }

  final String apiBaseUrl;

  static String _trimTrailingSlash(String value) {
    if (value.length > 1 && value.endsWith('/')) {
      return value.substring(0, value.length - 1);
    }
    return value;
  }
}
