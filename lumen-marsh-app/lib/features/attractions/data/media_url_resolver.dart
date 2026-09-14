String resolveMediaUrl(String baseUrl, String mediaPath) {
  if (mediaPath.startsWith('http://') || mediaPath.startsWith('https://')) {
    return mediaPath;
  }

  final normalizedBase = baseUrl.endsWith('/')
      ? baseUrl.substring(0, baseUrl.length - 1)
      : baseUrl;
  final normalizedPath = mediaPath.startsWith('/') ? mediaPath : '/$mediaPath';
  return '$normalizedBase$normalizedPath';
}
