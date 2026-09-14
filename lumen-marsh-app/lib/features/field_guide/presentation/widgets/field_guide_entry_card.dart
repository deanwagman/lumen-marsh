import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../design_system/foundations/lumen_radius.dart';
import '../../../../design_system/foundations/lumen_spacing.dart';
import '../../../../design_system/motion/lumen_pressable.dart';
import '../../domain/field_guide_entry.dart';
import 'field_guide_image.dart';

class FieldGuideEntryCard extends StatelessWidget {
  const FieldGuideEntryCard({super.key, required this.entry, this.onPressed});

  final FieldGuideEntry entry;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Card(
      clipBehavior: Clip.antiAlias,
      child: LumenPressable(
        key: ValueKey('field-guide-card-${entry.id}'),
        semanticLabel: '${entry.title}, ${entry.category.label}',
        borderRadius: LumenRadius.lgBorder,
        onPressed: onPressed ?? () => context.push('/guide/${entry.id}'),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Expanded(
              flex: 5,
              child: FieldGuideImage(
                assetPath: entry.imageAsset,
                altText: entry.imageAlt,
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(
                LumenSpacing.md,
                LumenSpacing.sm,
                LumenSpacing.md,
                LumenSpacing.md,
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  FieldGuideCategoryBadge(label: entry.category.label),
                  const SizedBox(height: LumenSpacing.xs),
                  Text(
                    entry.title,
                    style: theme.textTheme.titleLarge,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: LumenSpacing.xxs),
                  Text(
                    entry.summary,
                    style: theme.textTheme.bodyMedium?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
