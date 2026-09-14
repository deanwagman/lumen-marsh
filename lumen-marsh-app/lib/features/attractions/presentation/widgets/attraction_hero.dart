import 'package:flutter/material.dart';

import '../../../../design_system/foundations/lumen_breakpoints.dart';
import '../../../../design_system/foundations/lumen_spacing.dart';
import '../../../../design_system/theme/lumen_color_extension.dart';
import '../../domain/attraction_detail.dart';
import '../attraction_tone.dart';
import 'attraction_hero_image.dart';
import 'attraction_status_badge.dart';

class AttractionHero extends StatelessWidget {
  const AttractionHero({
    super.key,
    required this.detail,
    required this.apiBaseUrl,
    required this.isFavorite,
    required this.onFavoriteToggle,
  });

  final AttractionDetail detail;
  final String apiBaseUrl;
  final bool isFavorite;
  final VoidCallback onFavoriteToggle;

  @override
  Widget build(BuildContext context) {
    final wide = MediaQuery.sizeOf(context).width >= LumenBreakpoints.rail;
    final height = wide ? 280.0 : 200.0;
    final palette = context.lumenColors.paletteFor(
      toneForAttractionStatus(detail.status),
    );
    final imageUrl = heroImageUrl(
      apiBaseUrl: apiBaseUrl,
      heroPath: detail.experience.media.heroUrl,
    );

    return ClipRRect(
      child: SizedBox(
        height: height,
        width: double.infinity,
        child: Stack(
          fit: StackFit.expand,
          children: [
            AttractionHeroImage(
              imageUrl: imageUrl,
              altText: detail.experience.media.altText,
              fallbackIcon: placeholderIconFor(detail.type),
              fallbackColor: palette.container,
            ),
            const DecoratedBox(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [Colors.transparent, Color(0xCC000000)],
                ),
              ),
            ),
            Positioned(
              left: LumenSpacing.md,
              right: LumenSpacing.md,
              bottom: LumenSpacing.md,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  AttractionStatusBadge(status: detail.status),
                  const SizedBox(height: LumenSpacing.xs),
                  Text(
                    detail.name,
                    style: Theme.of(context).textTheme.headlineMedium
                        ?.copyWith(color: Colors.white),
                  ),
                  Text(
                    detail.experience.shortDescription,
                    style: Theme.of(context).textTheme.titleMedium
                        ?.copyWith(color: Colors.white70),
                  ),
                  Text(
                    '${detail.area} · ${detail.type.label}',
                    style: Theme.of(context).textTheme.bodyMedium
                        ?.copyWith(color: Colors.white60),
                  ),
                ],
              ),
            ),
            Positioned(
              top: LumenSpacing.sm,
              right: LumenSpacing.sm,
              child: Semantics(
                button: true,
                selected: isFavorite,
                label: isFavorite
                    ? 'Remove ${detail.name} from saved adventures'
                    : 'Save ${detail.name} to saved adventures',
                child: Material(
                  key: const Key('attraction-favorite-button'),
                  color: const Color(0xE6FAF7EF),
                  shape: const CircleBorder(),
                  child: SizedBox.square(
                    dimension: LumenBreakpoints.minTouchTarget,
                    child: IconButton(
                      tooltip: isFavorite
                          ? 'Remove from saved adventures'
                          : 'Save adventure',
                      onPressed: onFavoriteToggle,
                      color: Theme.of(context).colorScheme.primary,
                      icon: Icon(
                        isFavorite ? Icons.favorite : Icons.favorite_border,
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
