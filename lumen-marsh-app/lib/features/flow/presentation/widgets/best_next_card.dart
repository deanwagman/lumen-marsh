import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../app/config/app_config.dart';
import '../../../../design_system/components/status/lumen_status_badge.dart';
import '../../../../design_system/components/status/lumen_status_stripe.dart';
import '../../../../design_system/components/status/lumen_tone.dart';
import '../../../../design_system/foundations/lumen_radius.dart';
import '../../../../design_system/foundations/lumen_spacing.dart';
import '../../../../design_system/motion/lumen_pressable.dart';
import '../../../attractions/presentation/attraction_tone.dart';
import '../../../attractions/presentation/widgets/attraction_thumbnail.dart';
import '../../domain/best_next_ranker.dart';

class BestNextCard extends StatelessWidget {
  const BestNextCard({
    super.key,
    required this.recommendation,
    required this.originZoneId,
    this.onPressed,
  });

  final BestNextRecommendation recommendation;
  final String originZoneId;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final wait = recommendation.wait;
    final attraction = recommendation.attraction;
    final apiBaseUrl = context.read<AppConfig>().apiBaseUrl;
    final thumbnailUrl = thumbnailImageUrl(
      apiBaseUrl: apiBaseUrl,
      thumbnailPath: attraction?.thumbnailUrl,
    );
    final operating = wait.availability.isOperating;
    final tone = wait.isStale
        ? LumenTone.warning
        : (operating ? LumenTone.positive : LumenTone.neutral);

    return Card(
      clipBehavior: Clip.antiAlias,
      child: LumenPressable(
        semanticLabel: _semanticLabel,
        borderRadius: LumenRadius.lgBorder,
        onPressed: onPressed,
        child: IntrinsicHeight(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              LumenStatusStripe(tone: tone),
              AttractionThumbnail(
                imageUrl: thumbnailUrl,
                altText: attraction?.thumbnailAltText ?? wait.displayName,
                fallbackIcon: attraction == null
                    ? iconForAttractionId(wait.attractionId)
                    : placeholderIconFor(attraction.type),
              ),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.all(LumenSpacing.md),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Expanded(
                            child: Text(
                              wait.displayName,
                              style: theme.textTheme.titleLarge,
                            ),
                          ),
                          const SizedBox(width: LumenSpacing.sm),
                          LumenStatusBadge(
                            tone: tone,
                            label: wait.isStale
                                ? 'Stale wait'
                                : (operating ? 'Available' : 'Unavailable'),
                          ),
                        ],
                      ),
                      const SizedBox(height: LumenSpacing.xs),
                      Text(
                        wait.waitOutlook ??
                            (wait.postedWaitMinutes == null
                                ? 'Wait not posted'
                                : '${wait.postedWaitMinutes} min wait'),
                        style: theme.textTheme.titleSmall,
                      ),
                      Text(
                        wait.trend.label,
                        style: theme.textTheme.bodyMedium?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                      const SizedBox(height: LumenSpacing.sm),
                      Text(
                        recommendation.reason,
                        style: theme.textTheme.bodyMedium,
                      ),
                      const SizedBox(height: LumenSpacing.xxs),
                      Text(
                        recommendation.walkingLabel(originZoneId),
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                      if (wait.simulated) ...[
                        const SizedBox(height: LumenSpacing.xs),
                        LumenStatusBadge(
                          tone: LumenTone.informational,
                          label: 'Simulated',
                        ),
                      ],
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  String get _semanticLabel {
    final wait = recommendation.wait;
    final simulated = wait.simulated ? ', simulated demonstration data' : '';
    return '${wait.displayName}, ${wait.waitOutlook ?? wait.trend.label}, '
        '${recommendation.reason}$simulated';
  }
}

IconData iconForAttractionId(String attractionId) {
  return switch (attractionId) {
    'mangrove-run' => Icons.directions_boat_outlined,
    'stormglass-station' => Icons.theaters_outlined,
    'cypress-coil' => Icons.moving,
    _ => Icons.attractions_outlined,
  };
}
