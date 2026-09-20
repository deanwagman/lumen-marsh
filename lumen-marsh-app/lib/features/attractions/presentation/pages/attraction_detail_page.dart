import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../../app/config/app_config.dart';
import '../../../../core/errors/app_failure.dart';
import '../../../../core/formatters/timestamp_formatter.dart';
import '../../../../design_system/components/feedback/lumen_feedback.dart';
import '../../../../design_system/components/layout/lumen_page.dart';
import '../../../../design_system/components/status/lumen_notice_banner.dart';
import '../../../../design_system/components/status/lumen_tone.dart';
import '../../../../design_system/foundations/lumen_spacing.dart';
import '../../../../design_system/motion/lumen_entrance.dart';
import '../../../favorites/presentation/bloc/favorites_bloc.dart';
import '../../../favorites/presentation/bloc/favorites_event.dart';
import '../../../favorites/presentation/bloc/favorites_state.dart';
import '../../../advisories/presentation/bloc/advisories_bloc.dart';
import '../../../advisories/presentation/bloc/advisories_state.dart';
import '../../../advisories/presentation/bloc/advisory_catalog_merge.dart';
import '../../../advisories/presentation/widgets/guest_advisory_banner.dart';
import '../../../flow/presentation/bloc/flow_bloc.dart';
import '../../../flow/presentation/bloc/flow_state.dart';
import '../../../flow/presentation/flow_scope.dart';
import '../../../flow/presentation/widgets/wait_outlook.dart';
import '../../domain/attraction_detail.dart';
import '../../domain/attraction_summary.dart';
import '../bloc/attraction_detail_bloc.dart';
import '../bloc/attraction_detail_event.dart';
import '../bloc/attraction_detail_state.dart';
import '../bloc/attractions_bloc.dart';
import '../bloc/attractions_state.dart';
import '../widgets/attraction_fact_grid.dart';
import '../widgets/attraction_hero.dart';
import '../widgets/attraction_status_banner.dart';
import '../widgets/live_updating.dart';
import '../widgets/wait_time_display.dart';

class AttractionDetailPage extends StatelessWidget {
  const AttractionDetailPage({super.key, required this.attractionId});

  final String attractionId;

  @override
  Widget build(BuildContext context) {
    return LumenPage(
      title: 'Attraction',
      body: BlocBuilder<AttractionDetailBloc, AttractionDetailState>(
        builder: (context, state) {
          return switch (state) {
            AttractionDetailInitial() ||
            AttractionDetailLoading() => const LumenLoadingState(),
            AttractionDetailLoaded(:final detail) => LumenEntrance(
              child: _AttractionDetailBody(detail: detail),
            ),
            AttractionDetailFailure(:final failure) => _failureView(
              context,
              failure,
            ),
          };
        },
      ),
    );
  }

  Widget _failureView(BuildContext context, AppFailure failure) {
    void retry() {
      context.read<AttractionDetailBloc>().add(
        AttractionDetailRequested(attractionId),
      );
    }

    if (failure is NetworkFailure) {
      return LumenOfflineState(message: failure.message, onRetry: retry);
    }
    return LumenErrorState(message: failure.message, onRetry: retry);
  }
}

class _AttractionDetailBody extends StatelessWidget {
  const _AttractionDetailBody({required this.detail});

  final AttractionDetail detail;

  AttractionSummary? _liveSummary(AttractionsState state) {
    final attractions = switch (state) {
      AttractionsLoaded(:final attractions) => attractions,
      AttractionsRefreshing(:final attractions) => attractions,
      AttractionsFailure(:final previous) => previous,
      _ => null,
    };
    if (attractions == null) {
      return null;
    }
    for (final attraction in attractions) {
      if (attraction.id == detail.id) {
        return attraction;
      }
    }
    return null;
  }

  @override
  Widget build(BuildContext context) {
    final apiBaseUrl = context.read<AppConfig>().apiBaseUrl;
    final favoritesState = context.watch<FavoritesBloc>().state;
    final isFavorite =
        favoritesState is FavoritesLoaded &&
        favoritesState.isFavorite(detail.id);
    final liveSummary = _liveSummary(context.watch<AttractionsBloc>().state);
    final effective = liveSummary == null
        ? detail
        : detail.withSummary(liveSummary);
    final advisoryState = context.watch<AdvisoriesBloc>().state;
    final relevantAdvisories = switch (advisoryState) {
      AdvisoriesLoaded(:final advisories) => advisoriesForAttraction(
        advisories,
        detail.id,
      ),
      AdvisoriesRefreshing(:final advisories) => advisoriesForAttraction(
        advisories,
        detail.id,
      ),
      AdvisoriesFailure(:final previous) =>
        previous == null
            ? const []
            : advisoriesForAttraction(previous, detail.id),
      _ => const [],
    };

    return LumenContentConstraint(
      child: ListView(
        padding: LumenSpacing.headerInsets,
        children: [
          AttractionHero(
            detail: effective,
            apiBaseUrl: apiBaseUrl,
            isFavorite: isFavorite,
            onFavoriteToggle: () =>
                context.read<FavoritesBloc>().add(FavoriteToggled(detail.id)),
          ),
          const SizedBox(height: LumenSpacing.lg),
          if (!effective.status.isOperating) ...[
            AttractionStatusBanner(attraction: effective.summary),
            const SizedBox(height: LumenSpacing.lg),
          ],
          LiveUpdating(
            animationKey:
                'detail-wait-${effective.id}-${effective.version}-${effective.waitMinutes}',
            child: WaitTimeDisplay(
              label: effective.waitTimeLabel,
              emphasized: true,
            ),
          ),
          const SizedBox(height: LumenSpacing.xxs),
          LiveUpdating(
            animationKey: 'detail-updated-${effective.id}-${effective.version}',
            child: Text(
              'Updated ${formatUpdatedAt(effective.updatedAt)}',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ),
          ),
          _WaitOutlookSection(
            attractionId: effective.id,
            isOperating: effective.status.isOperating,
          ),
          if (relevantAdvisories.isNotEmpty) ...[
            const SizedBox(height: LumenSpacing.lg),
            for (final advisory in relevantAdvisories) ...[
              GuestAdvisoryBanner(
                key: Key('detail-advisory-${advisory.id}'),
                advisory: advisory,
                compact: true,
                onPressed: () => context.push('/advisories/${advisory.id}'),
              ),
              const SizedBox(height: LumenSpacing.sm),
            ],
          ],
          const SizedBox(height: LumenSpacing.lg),
          AttractionFactGrid(detail: effective),
        ],
      ),
    );
  }
}

class _WaitOutlookSection extends StatelessWidget {
  const _WaitOutlookSection({
    required this.attractionId,
    required this.isOperating,
  });

  final String attractionId;
  final bool isOperating;

  @override
  Widget build(BuildContext context) {
    final flowBloc = maybeFlowBloc(context);
    if (flowBloc == null) {
      return const SizedBox.shrink();
    }
    return BlocBuilder<FlowBloc, FlowState>(
      builder: (context, state) {
        if (state is! FlowLoaded) {
          return const SizedBox.shrink();
        }
        final wait = state.waitFor(attractionId);
        final alternatives = isOperating ? const [] : state.guidance;
        return Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (wait != null) ...[
              const SizedBox(height: LumenSpacing.lg),
              WaitOutlook(wait: wait),
            ],
            if (!isOperating && alternatives.isNotEmpty) ...[
              const SizedBox(height: LumenSpacing.lg),
              LumenSection(
                title: 'Published alternatives',
                child: Column(
                  children: [
                    for (final item in alternatives) ...[
                      LumenNoticeBanner(
                        tone: LumenTone.informational,
                        title: 'Park guidance',
                        message:
                            item.guestMessage ??
                            'Park operators published an alternate experience.',
                        footer: item.simulated
                            ? 'Simulated demonstration data'
                            : null,
                      ),
                      const SizedBox(height: LumenSpacing.sm),
                    ],
                  ],
                ),
              ),
            ],
          ],
        );
      },
    );
  }
}
