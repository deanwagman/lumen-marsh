import 'package:flutter/material.dart';

import '../../data/media_url_resolver.dart';

class AttractionHeroImage extends StatelessWidget {
  const AttractionHeroImage({
    super.key,
    required this.imageUrl,
    required this.altText,
    required this.fallbackIcon,
    required this.fallbackColor,
  });

  final String imageUrl;
  final String altText;
  final IconData fallbackIcon;
  final Color fallbackColor;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      image: true,
      label: altText,
      child: Image.network(
        imageUrl,
        fit: BoxFit.cover,
        loadingBuilder: (context, child, loadingProgress) {
          if (loadingProgress == null) {
            return child;
          }
          return ColoredBox(
            color: fallbackColor,
            child: Center(
              child: CircularProgressIndicator(
                value: loadingProgress.expectedTotalBytes == null
                    ? null
                    : loadingProgress.cumulativeBytesLoaded /
                          loadingProgress.expectedTotalBytes!,
              ),
            ),
          );
        },
        errorBuilder: (context, error, stackTrace) {
          return ColoredBox(
            color: fallbackColor,
            child: Center(
              child: Icon(
                fallbackIcon,
                size: 88,
                color: Theme.of(context).colorScheme.onSurface
                    .withValues(alpha: 0.55),
              ),
            ),
          );
        },
      ),
    );
  }
}

String heroImageUrl({required String apiBaseUrl, required String heroPath}) {
  return resolveMediaUrl(apiBaseUrl, heroPath);
}
