import 'package:flutter/material.dart';

import '../../data/media_url_resolver.dart';

class AttractionThumbnail extends StatelessWidget {
  const AttractionThumbnail({
    super.key,
    required this.imageUrl,
    required this.altText,
    required this.fallbackIcon,
    this.width = 128,
  });

  final String? imageUrl;
  final String? altText;
  final IconData fallbackIcon;
  final double width;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    if (imageUrl == null || imageUrl!.isEmpty) {
      return _Fallback(
        width: width,
        icon: fallbackIcon,
        color: theme.colorScheme.surfaceContainer,
      );
    }

    return Semantics(
      image: true,
      label: altText ?? '',
      child: SizedBox(
        width: width,
        child: Image.network(
          imageUrl!,
          fit: BoxFit.cover,
          alignment: Alignment.center,
          errorBuilder: (context, error, stackTrace) {
            return _Fallback(
              width: width,
              icon: fallbackIcon,
              color: theme.colorScheme.surfaceContainer,
            );
          },
        ),
      ),
    );
  }
}

class _Fallback extends StatelessWidget {
  const _Fallback({
    required this.width,
    required this.icon,
    required this.color,
  });

  final double width;
  final IconData icon;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return ColoredBox(
      color: color,
      child: SizedBox(
        width: width,
        child: Center(
          child: Icon(
            icon,
            size: 36,
            color: Theme.of(context).colorScheme.primary,
          ),
        ),
      ),
    );
  }
}

String? thumbnailImageUrl({
  required String apiBaseUrl,
  required String? thumbnailPath,
}) {
  if (thumbnailPath == null || thumbnailPath.isEmpty) {
    return null;
  }
  return resolveMediaUrl(apiBaseUrl, thumbnailPath);
}
