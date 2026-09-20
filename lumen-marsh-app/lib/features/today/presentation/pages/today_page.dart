import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../advisories/presentation/bloc/advisories_event.dart';
import '../../../advisories/presentation/bloc/advisories_bloc.dart';
import '../../../advisories/presentation/bloc/advisories_state.dart';
import '../../../advisories/presentation/widgets/guest_advisory_banner.dart';
import '../../../attractions/domain/attraction_summary.dart';
import '../../../attractions/presentation/bloc/attractions_bloc.dart';
import '../../../attractions/presentation/bloc/attractions_event.dart';
import '../../../attractions/presentation/bloc/attractions_state.dart';
import '../../../attractions/presentation/widgets/attraction_card.dart';
import '../../../attractions/presentation/widgets/attraction_status_banner.dart';
import '../../../attractions/presentation/widgets/live_connection_banner.dart';
import '../../../favorites/presentation/bloc/favorites_bloc.dart';
import '../../../favorites/presentation/bloc/favorites_state.dart';
import '../../../flow/presentation/bloc/flow_bloc.dart';
import '../../../flow/presentation/bloc/flow_event.dart';
import '../../../flow/presentation/bloc/flow_state.dart';
import '../../../flow/presentation/flow_scope.dart';
import '../../../flow/presentation/widgets/best_next_section.dart';
import '../../../../core/errors/app_failure.dart';
import '../../../../core/formatters/timestamp_formatter.dart';
import '../../../../design_system/components/feedback/lumen_feedback.dart';
import '../../../../design_system/components/layout/lumen_page.dart';
import '../../../../design_system/components/status/lumen_fact.dart';
import '../../../../design_system/components/status/lumen_notice_banner.dart';
import '../../../../design_system/components/status/lumen_tone.dart';
import '../../../../design_system/foundations/lumen_breakpoints.dart';
import '../../../../design_system/foundations/lumen_spacing.dart';
import '../today_view_model.dart';

class TodayPage extends StatelessWidget {
  const TodayPage({super.key});

  @override
  Widget build(BuildContext context) {
    return LumenPage(
      title: 'Today',
      actions: [
        IconButton(
          tooltip: 'Refresh park conditions',
          onPressed: () {
            context.read<AttractionsBloc>().add(const AttractionsRefreshed());
            context.read<AdvisoriesBloc>().add(const AdvisoriesRefreshed());
            maybeFlowBloc(context)?.add(const FlowRefreshed());
          },
          icon: const Icon(Icons.refresh),
        ),
      ],
      body: BlocBuilder<AttractionsBloc, AttractionsState>(
        builder: (context, state) {
          return switch (state) {
            AttractionsInitial() ||
            AttractionsLoading() => const LumenLoadingState(),
            AttractionsEmpty() => LumenEmptyState(
              title: 'No attractions yet',
              message: 'The park catalog is quiet right now. Refresh when VenueOps has listings.',
              onRetry: () => context.read<AttractionsBloc>().add(
                const AttractionsRequested(),
              ),
            ),
            AttractionsLoaded() ||
            AttractionsRefreshing() => _TodayDashboard(state: state),
            AttractionsFailure(:final failure, :final previous) =>
              previous != null && previous.isNotEmpty
                  ? _TodayDashboard(
                      state: AttractionsLoaded(previous),
                      bannerMessage: failure.message,
                    )
                  : _failureView(context, failure),
          };
        },
      ),
    );
  }

  Widget _failureView(BuildContext context, AppFailure failure) {
    void retry() {
      context.read<AttractionsBloc>().add(const AttractionsRequested());
    }

    if (failure is NetworkFailure) {
      return LumenOfflineState(message: failure.message, onRetry: retry);
    }
    return LumenErrorState(message: failure.message, onRetry: retry);
  }
}

class _TodayDashboard extends StatelessWidget {
  const _TodayDashboard({required this.state, this.bannerMessage});

  final AttractionsState state;
  final String? bannerMessage;

  List<AttractionSummary> get attractions => switch (state) {
    AttractionsLoaded(:final attractions) => attractions,
    AttractionsRefreshing(:final attractions) => attractions,
    _ => const [],
  };

  @override
  Widget build(BuildContext context) {
    return BlocBuilder<FavoritesBloc, FavoritesState>(
      builder: (context, favoritesState) {
        final favoriteIds = favoritesState is FavoritesLoaded
            ? favoritesState.favoriteIds
            : const <String>[];
        final viewModel = TodayViewModel.from(
          attractions: attractions,
          favoriteIds: favoriteIds,
        );
        final wide = MediaQuery.sizeOf(context).width >= LumenBreakpoints.rail;

        return RefreshIndicator(
          onRefresh: () {
            final attractionsBloc = context.read<AttractionsBloc>();
            final advisoriesBloc = context.read<AdvisoriesBloc>();
            final flowBloc = maybeFlowBloc(context);
            attractionsBloc.add(const AttractionsRefreshed());
            advisoriesBloc.add(const AdvisoriesRefreshed());
            flowBloc?.add(const FlowRefreshed());
            return Future.wait([
              attractionsBloc.stream.firstWhere(
                (state) =>
                    state is AttractionsLoaded ||
                    state is AttractionsEmpty ||
                    state is AttractionsFailure,
              ),
              advisoriesBloc.stream.firstWhere(
                (state) =>
                    state is AdvisoriesLoaded ||
                    state is AdvisoriesEmpty ||
                    state is AdvisoriesFailure,
              ),
              if (flowBloc != null)
                flowBloc.stream.firstWhere(
                  (state) =>
                      state is FlowLoaded ||
                      state is FlowEmpty ||
                      state is FlowFailure,
                ),
            ]);
          },
          child: ListView(
            key: const Key('today-dashboard'),
            physics: const AlwaysScrollableScrollPhysics(),
            padding: LumenSpacing.pageInsets,
            children: [
              LumenPageHeader(
                title: 'Lumen Marsh',
                subtitle: viewModel.parkSummary,
              ),
              const SizedBox(height: LumenSpacing.md),
              LiveConnectionBanner(state: state),
              BlocBuilder<AdvisoriesBloc, AdvisoriesState>(
                builder: (context, advisoryState) {
                  final advisories = switch (advisoryState) {
                    AdvisoriesLoaded(:final advisories) => advisories,
                    AdvisoriesRefreshing(:final advisories) => advisories,
                    AdvisoriesFailure(:final previous) => previous ?? const [],
                    _ => const [],
                  };
                  if (advisories.isEmpty) {
                    return const SizedBox.shrink();
                  }
                  return Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const SizedBox(height: LumenSpacing.md),
                      LumenSection(
                        title: 'Park advisories',
                        child: Column(
                          children: [
                            for (var i = 0; i < advisories.length; i++) ...[
                              GuestAdvisoryBanner(
                                key: Key('today-advisory-${advisories[i].id}'),
                                advisory: advisories[i],
                                compact: true,
                                onPressed: () => context.push(
                                  '/advisories/${advisories[i].id}',
                                ),
                                onAttractionPressed: (id) =>
                                    context.push('/attractions/$id'),
                              ),
                              if (i != advisories.length - 1)
                                const SizedBox(height: LumenSpacing.sm),
                            ],
                            const SizedBox(height: LumenSpacing.sm),
                            Align(
                              alignment: Alignment.centerLeft,
                              child: TextButton(
                                onPressed: () => context.push('/advisories'),
                                child: Text(
                                  'View all advisories (${advisories.length})',
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  );
                },
              ),
              if (bannerMessage != null) ...[
                const SizedBox(height: LumenSpacing.md),
                LumenNoticeBanner(
                  tone: LumenTone.critical,
                  message: bannerMessage!,
                ),
              ],
              const SizedBox(height: LumenSpacing.lg),
              LumenSection(
                title: 'Park conditions',
                child: _SummaryFacts(viewModel: viewModel, wide: wide),
              ),
              BestNextSection(
                catalog: attractions,
                favoriteIds: favoriteIds.toSet(),
                onAttractionPressed: (id) => context.push('/attractions/$id'),
              ),
              if (viewModel.savedAdventures.isNotEmpty) ...[
                const SizedBox(height: LumenSpacing.lg),
                LumenSection(
                  title: 'Saved Adventures',
                  child: Column(
                    children: [
                      for (
                        var i = 0;
                        i < viewModel.savedAdventures.length;
                        i++
                      ) ...[
                        AttractionCard(
                          key: Key(
                            'saved-adventure-${viewModel.savedAdventures[i].id}',
                          ),
                          attraction: viewModel.savedAdventures[i],
                          isFavorite: true,
                          onPressed: () => context.push(
                            '/attractions/${viewModel.savedAdventures[i].id}',
                          ),
                        ),
                        if (i != viewModel.savedAdventures.length - 1)
                          const SizedBox(height: LumenSpacing.sm),
                      ],
                    ],
                  ),
                ),
              ],
              if (viewModel.activeNotices.isNotEmpty) ...[
                const SizedBox(height: LumenSpacing.lg),
                LumenSection(
                  title: 'Active notices',
                  child: Column(
                    children: [
                      for (
                        var i = 0;
                        i < viewModel.activeNotices.length;
                        i++
                      ) ...[
                        AttractionStatusBanner(
                          attraction: viewModel.activeNotices[i],
                        ),
                        if (i != viewModel.activeNotices.length - 1)
                          const SizedBox(height: LumenSpacing.sm),
                      ],
                    ],
                  ),
                ),
              ],
              if (viewModel.recommended != null) ...[
                const SizedBox(height: LumenSpacing.lg),
                _CatalogFallbackRecommendation(
                  attraction: viewModel.recommended!,
                  isFavorite: favoriteIds.contains(viewModel.recommended!.id),
                ),
              ],
              const SizedBox(height: LumenSpacing.lg),
              OutlinedButton(
                onPressed: () => context.go('/attractions'),
                child: const Text('View all attractions'),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _CatalogFallbackRecommendation extends StatelessWidget {
  const _CatalogFallbackRecommendation({
    required this.attraction,
    required this.isFavorite,
  });

  final AttractionSummary attraction;
  final bool isFavorite;

  @override
  Widget build(BuildContext context) {
    final flowBloc = maybeFlowBloc(context);
    if (flowBloc == null) {
      return _card(context);
    }
    return BlocBuilder<FlowBloc, FlowState>(
      builder: (context, state) {
        if (_hasPublishedFlowSuggestions(state)) {
          return const SizedBox.shrink();
        }
        return _card(context);
      },
    );
  }

  Widget _card(BuildContext context) {
    return LumenSection(
      title: 'Best next adventure',
      child: AttractionCard(
        key: const Key('today-recommendation'),
        attraction: attraction,
        isFavorite: isFavorite,
        onPressed: () => context.push('/attractions/${attraction.id}'),
      ),
    );
  }
}

bool _hasPublishedFlowSuggestions(FlowState state) {
  if (state is! FlowLoaded) {
    return false;
  }
  return state.guidance.isNotEmpty ||
      state.waits.any((wait) => wait.availability.isOperating);
}

class _SummaryFacts extends StatelessWidget {
  const _SummaryFacts({required this.viewModel, required this.wide});

  final TodayViewModel viewModel;
  final bool wide;

  @override
  Widget build(BuildContext context) {
    final facts = [
      LumenFact(
        label: 'Operating now',
        value: '${viewModel.operatingCount} of ${viewModel.totalCount}',
        icon: Icons.attractions_outlined,
      ),
      LumenFact(
        label: 'Shortest wait',
        value: viewModel.shortestWaitLabel,
        icon: Icons.schedule,
      ),
      LumenFact(
        label: 'Last updated',
        value: viewModel.lastUpdatedAt == null
            ? 'Not available'
            : formatUpdatedAt(viewModel.lastUpdatedAt!),
        icon: Icons.update,
      ),
    ];

    if (!wide) {
      return Column(
        children: [
          for (var i = 0; i < facts.length; i++) ...[
            facts[i],
            if (i != facts.length - 1) const SizedBox(height: LumenSpacing.md),
          ],
        ],
      );
    }

    return GridView.count(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      crossAxisCount:
          MediaQuery.sizeOf(context).width >= LumenBreakpoints.expanded ? 3 : 2,
      childAspectRatio: 2.6,
      mainAxisSpacing: LumenSpacing.md,
      crossAxisSpacing: LumenSpacing.md,
      children: facts,
    );
  }
}
