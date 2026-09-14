import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/config/app_config.dart';
import '../../../../design_system/components/status/lumen_status_stripe.dart';
import '../../../../design_system/foundations/lumen_radius.dart';
import '../../../../design_system/foundations/lumen_spacing.dart';
import '../../../../design_system/motion/lumen_pressable.dart';
import '../../../../core/formatters/timestamp_formatter.dart';
import '../../domain/attraction_summary.dart';
import '../attraction_tone.dart';
import 'attraction_status_badge.dart';
import 'attraction_thumbnail.dart';
import 'live_updating.dart';
import 'wait_time_display.dart';

class AttractionCard extends StatelessWidget {
  const AttractionCard({
    super.key,
    required this.attraction,
    this.isFavorite = false,
    this.onPressed,
  });

  final AttractionSummary attraction;
  final bool isFavorite;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final tone = toneForAttractionStatus(attraction.status);
    final apiBaseUrl = context.read<AppConfig>().apiBaseUrl;
    final thumbnailUrl = thumbnailImageUrl(
      apiBaseUrl: apiBaseUrl,
      thumbnailPath: attraction.thumbnailUrl,
    );

    return Card(
      clipBehavior: Clip.antiAlias,
      child: LumenPressable(
        key: ValueKey('attraction-card-${attraction.id}'),
        semanticLabel: _semanticLabel,
        borderRadius: LumenRadius.lgBorder,
        onPressed:
            onPressed ?? () => context.push('/attractions/${attraction.id}'),
        child: IntrinsicHeight(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              LumenStatusStripe(tone: tone),
              AttractionThumbnail(
                imageUrl: thumbnailUrl,
                altText: attraction.thumbnailAltText,
                fallbackIcon: placeholderIconFor(attraction.type),
              ),
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.all(LumenSpacing.md),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Expanded(
                            child: Text(
                              attraction.name,
                              style: theme.textTheme.titleLarge,
                            ),
                          ),
                          if (isFavorite) ...[
                            const SizedBox(width: LumenSpacing.xs),
                            Semantics(
                              label: 'Saved adventure',
                              child: Icon(
                                Icons.favorite,
                                size: 20,
                                color: theme.colorScheme.primary,
                              ),
                            ),
                          ],
                          const SizedBox(width: LumenSpacing.sm),
                          LiveUpdating(
                            animationKey:
                                'status-${attraction.id}-${attraction.version}-${attraction.status}',
                            child: AttractionStatusBadge(
                              status: attraction.status,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 6),
                      Text(
                        '${attraction.area} · ${attraction.type.label}',
                        style: theme.textTheme.bodyMedium?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                      const SizedBox(height: LumenSpacing.sm),
                      LiveUpdating(
                        animationKey:
                            'wait-${attraction.id}-${attraction.version}-${attraction.waitMinutes}',
                        child: WaitTimeDisplay(
                          label: attraction.waitTimeLabel,
                          emphasized: attraction.status.isOperating,
                        ),
                      ),
                      Text(
                        attraction.capacityLabel,
                        style: theme.textTheme.bodyMedium,
                      ),
                      const SizedBox(height: LumenSpacing.xxs),
                      LiveUpdating(
                        animationKey:
                            'updated-${attraction.id}-${attraction.version}',
                        child: Text(
                          'Updated ${formatUpdatedAt(attraction.updatedAt)}',
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: theme.colorScheme.onSurfaceVariant,
                          ),
                        ),
                      ),
                      if (attraction.statusMessage != null) ...[
                        const SizedBox(height: 10),
                        Text(
                          attraction.statusMessage!,
                          style: theme.textTheme.bodyMedium,
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
    final favoriteSuffix = isFavorite ? ', saved adventure' : '';
    return '${attraction.name}, ${attraction.status.label}$favoriteSuffix';
  }
}
