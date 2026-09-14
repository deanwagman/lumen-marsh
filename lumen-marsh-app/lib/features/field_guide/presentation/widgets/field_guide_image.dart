import 'package:flutter/material.dart';

import '../../../../design_system/foundations/lumen_radius.dart';

class FieldGuideImage extends StatelessWidget {
  const FieldGuideImage({
    super.key,
    required this.assetPath,
    required this.altText,
    this.height,
    this.fit = BoxFit.cover,
    this.fallbackIcon = Icons.auto_stories_outlined,
  });

  final String assetPath;
  final String altText;
  final double? height;
  final BoxFit fit;
  final IconData fallbackIcon;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final image = Image.asset(
      assetPath,
      fit: fit,
      width: double.infinity,
      height: height ?? double.infinity,
      errorBuilder: (context, error, stackTrace) {
        return ColoredBox(
          color: theme.colorScheme.surfaceContainer,
          child: Center(
            child: Icon(
              fallbackIcon,
              size: 48,
              color: theme.colorScheme.primary,
            ),
          ),
        );
      },
    );

    return Semantics(
      image: true,
      label: altText,
      child: height == null
          ? SizedBox.expand(child: image)
          : SizedBox(height: height, width: double.infinity, child: image),
    );
  }
}

class FieldGuideCategoryBadge extends StatelessWidget {
  const FieldGuideCategoryBadge({super.key, required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: theme.colorScheme.surfaceContainerHigh,
        borderRadius: LumenRadius.pillBorder,
      ),
      child: Text(
        label,
        style: theme.textTheme.labelMedium?.copyWith(
          color: theme.colorScheme.onSurfaceVariant,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}
