import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../../design_system/components/feedback/lumen_feedback.dart';
import '../../../../design_system/components/layout/lumen_page.dart';
import '../../../../design_system/foundations/lumen_spacing.dart';
import '../../domain/guest_advisory.dart';
import '../bloc/advisories_bloc.dart';
import '../bloc/advisories_state.dart';
import '../widgets/guest_advisory_banner.dart';

/// Thin detail shell: resolves the advisory from [AdvisoriesBloc] catalog.
class AdvisoryDetailPage extends StatelessWidget {
  const AdvisoryDetailPage({super.key, required this.advisoryId});

  final String advisoryId;

  @override
  Widget build(BuildContext context) {
    return LumenPage(
      title: 'Advisory',
      body: BlocBuilder<AdvisoriesBloc, AdvisoriesState>(
        builder: (context, state) {
          final advisory = _findAdvisory(state, advisoryId);
          if (advisory != null) {
            return ListView(
              padding: LumenSpacing.pageInsets,
              children: [
                AdvisoryContent(
                  key: Key('advisory-detail-${advisory.id}'),
                  advisory: advisory,
                  onAttractionPressed: (id) => context.push('/attractions/$id'),
                ),
              ],
            );
          }

          if (state is AdvisoriesLoading || state is AdvisoriesInitial) {
            return const LumenLoadingState(message: 'Opening advisory…');
          }

          return LumenEmptyState(
            title: 'Advisory cleared',
            message: 'This park advisory is no longer active. It may have been withdrawn or the related incident was resolved.',
            actionLabel: 'Back to advisories',
            icon: Icons.notifications_off_outlined,
            onRetry: () => context.go('/advisories'),
          );
        },
      ),
    );
  }
}

GuestAdvisory? _findAdvisory(AdvisoriesState state, String id) {
  final advisories = switch (state) {
    AdvisoriesLoaded(:final advisories) => advisories,
    AdvisoriesRefreshing(:final advisories) => advisories,
    AdvisoriesFailure(:final previous) => previous ?? const [],
    _ => const <GuestAdvisory>[],
  };
  for (final advisory in advisories) {
    if (advisory.id == id) {
      return advisory;
    }
  }
  return null;
}
