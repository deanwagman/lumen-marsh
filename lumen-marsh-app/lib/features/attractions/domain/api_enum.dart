T enumFromApi<T extends Enum>(List<T> values, String raw) {
  final camel = snakeToCamel(raw);
  for (final value in values) {
    if (value.name == camel) {
      return value;
    }
  }
  throw FormatException('Unknown ${T.toString()} value: $raw');
}

String snakeToCamel(String raw) {
  final parts = raw.toLowerCase().split('_');
  if (parts.isEmpty) {
    return raw;
  }
  final rest = parts.skip(1).map((part) {
    if (part.isEmpty) {
      return part;
    }
    return '${part[0].toUpperCase()}${part.substring(1)}';
  }).join();
  return '${parts.first}$rest';
}
