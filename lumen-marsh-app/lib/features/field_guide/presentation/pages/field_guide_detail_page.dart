import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../../design_system/components/feedback/lumen_feedback.dart';
import '../../../../design_system/components/layout/lumen_page.dart';
import '../../../../design_system/foundations/lumen_breakpoints.dart';
import '../../../../design_system/foundations/lumen_spacing.dart';
import '../../domain/field_guide_entry.dart';
import '../../domain/field_guide_repository.dart';
import '../widgets/field_guide_image.dart';

class FieldGuideDetailPage extends StatelessWidget {
  const FieldGuideDetailPage({super.key, required this.entryId});

  final String entryId;

  @override
  Widget build(BuildContext context) {
    return LumenPage(
      title: 'Field Guide',
      body: FutureBuilder<FieldGuideEntry?>(
        key: ValueKey('field-guide-detail-$entryId'),
        future: context.read<FieldGuideRepository>().getById(entryId),
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const LumenLoadingState(message: 'Opening entry…');
          }

          final entry = snapshot.data;
          if (entry == null) {
            return LumenEmptyState(
              title: 'Entry not found',
              message: 'This field note is missing from the journal. Return to the catalog to keep exploring.',
              actionLabel: 'Back to Field Guide',
              icon: Icons.auto_stories_outlined,
              onRetry: () => context.go('/guide'),
            );
          }

          return _FieldGuideDetailBody(entry: entry);
        },
      ),
    );
  }
}

class _FieldGuideDetailBody extends StatelessWidget {
  const _FieldGuideDetailBody({required this.entry});

  final FieldGuideEntry entry;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final wide = MediaQuery.sizeOf(context).width >= LumenBreakpoints.rail;

    return LumenContentConstraint(
      maxWidth: LumenBreakpoints.pageMaxWidth,
      child: ListView(
        key: Key('field-guide-detail-${entry.id}'),
        padding: LumenSpacing.headerInsets,
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(12),
            child: FieldGuideImage(
              assetPath: entry.imageAsset,
              altText: entry.imageAlt,
              height: wide ? 280 : 200,
            ),
          ),
          const SizedBox(height: LumenSpacing.lg),
          FieldGuideCategoryBadge(label: entry.category.label),
          const SizedBox(height: LumenSpacing.sm),
          Text(entry.title, style: theme.textTheme.headlineMedium),
          const SizedBox(height: LumenSpacing.sm),
          Text(
            entry.summary,
            style: theme.textTheme.titleMedium?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: LumenSpacing.lg),
          for (var i = 0; i < entry.bodyParagraphs.length; i++) ...[
            Text(entry.bodyParagraphs[i], style: theme.textTheme.bodyLarge),
            if (i != entry.bodyParagraphs.length - 1)
              const SizedBox(height: LumenSpacing.md),
          ],
        ],
      ),
    );
  }
}
