import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/errors/app_failure.dart';
import '../../../../core/formatters/timestamp_formatter.dart';
import '../../../../core/venue/domain/venue_live.dart';
import '../../../../design_system/components/feedback/lumen_feedback.dart';
import '../../../../design_system/components/layout/lumen_page.dart';
import '../../../../design_system/foundations/lumen_spacing.dart';
import '../../../attractions/presentation/bloc/attractions_bloc.dart';
import '../../../attractions/presentation/bloc/attractions_state.dart';
import '../../../attractions/presentation/widgets/live_connection_banner.dart';
import '../../domain/advisory_severity.dart';
import '../../domain/guest_advisory.dart';
import '../bloc/advisories_bloc.dart';
import '../bloc/advisories_event.dart';
import '../bloc/advisories_state.dart';
import '../widgets/guest_advisory_banner.dart';

class AdvisoriesPage extends StatelessWidget {
  const AdvisoriesPage({super.key});

  @override
  Widget build(BuildContext context) {
    return LumenPage(
      title: 'Advisories',
      actions: [
        IconButton(
          tooltip: 'Refresh advisories',
          onPressed: () =>
              context.read<AdvisoriesBloc>().add(const AdvisoriesRefreshed()),
          icon: const Icon(Icons.refresh),
        ),
      ],
      body: BlocBuilder<AdvisoriesBloc, AdvisoriesState>(
        builder: (context, state) {
          return switch (state) {
            AdvisoriesInitial() ||
            AdvisoriesLoading() => const LumenLoadingState(),
            AdvisoriesEmpty() => _emptyState(context, state),
            AdvisoriesLoaded() ||
            AdvisoriesRefreshing() => _AdvisoriesList(state: state),
            AdvisoriesFailure(:final failure, :final previous) =>
              previous != null
                  ? _AdvisoriesList(
                      state: AdvisoriesLoaded(previous),
                      bannerMessage: failure.message,
                    )
                  : _failureView(context, failure),
          };
        },
      ),
    );
  }

  Widget _emptyState(BuildContext context, AdvisoriesEmpty state) {
    return LumenEmptyState(
      title: 'No active park advisories',
      message: state.connectionStatus == LiveConnectionStatus.stale
          ? 'Last known advisories could not be verified. Pull to refresh when connection returns.'
          : 'There are no published guest advisories right now.',
      onRetry: () =>
          context.read<AdvisoriesBloc>().add(const AdvisoriesRefreshed()),
    );
  }

  Widget _failureView(BuildContext context, AppFailure failure) {
    void retry() {
      context.read<AdvisoriesBloc>().add(const AdvisoriesRequested());
    }

    if (failure is NetworkFailure) {
      return LumenOfflineState(message: failure.message, onRetry: retry);
    }
    return LumenErrorState(message: failure.message, onRetry: retry);
  }
}

class _AdvisoriesList extends StatelessWidget {
  const _AdvisoriesList({required this.state, this.bannerMessage});

  final AdvisoriesState state;
  final String? bannerMessage;

  List<GuestAdvisory> get advisories => switch (state) {
    AdvisoriesLoaded(:final advisories) => advisories,
    AdvisoriesRefreshing(:final advisories) => advisories,
    _ => const [],
  };

  DateTime? get lastSyncedAt => switch (state) {
    AdvisoriesLoaded(:final lastSyncedAt) => lastSyncedAt,
    AdvisoriesRefreshing(:final lastSyncedAt) => lastSyncedAt,
    _ => null,
  };

  Set<String> get clearedIds => switch (state) {
    AdvisoriesLoaded(:final clearedAdvisoryIds) => clearedAdvisoryIds,
    AdvisoriesRefreshing(:final clearedAdvisoryIds) => clearedAdvisoryIds,
    _ => const {},
  };

  @override
  Widget build(BuildContext context) {
    final attractionsState = context.watch<AttractionsBloc>().state;
    final connectionState = attractionsState is AttractionsLoaded
        ? attractionsState
        : attractionsState is AttractionsRefreshing
        ? attractionsState
        : null;

    return RefreshIndicator(
      onRefresh: () {
        final bloc = context.read<AdvisoriesBloc>();
        bloc.add(const AdvisoriesRefreshed());
        return bloc.stream.firstWhere(
          (state) =>
              state is AdvisoriesLoaded ||
              state is AdvisoriesEmpty ||
              state is AdvisoriesFailure,
        );
      },
      child: ListView(
        padding: LumenSpacing.pageInsets,
        children: [
          Text(
            '${advisories.length} active ${advisories.length == 1 ? 'advisory' : 'advisories'}',
            style: Theme.of(context).textTheme.titleMedium,
          ),
          if (lastSyncedAt != null) ...[
            const SizedBox(height: LumenSpacing.xxs),
            Text(
              'Last synchronized ${formatUpdatedAt(lastSyncedAt!)}',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ),
          ],
          if (connectionState != null) ...[
            const SizedBox(height: LumenSpacing.md),
            LiveConnectionBanner(state: connectionState),
          ],
          if (bannerMessage != null) ...[
            const SizedBox(height: LumenSpacing.md),
            GuestAdvisoryBanner(
              advisory: GuestAdvisory(
                id: 'load-failure',
                severity: AdvisorySeverity.major,
                title: 'Advisories unavailable',
                message: bannerMessage!,
                affectedAttractionIds: const [],
                updatedAt: DateTime.now().toUtc(),
                version: 0,
              ),
            ),
          ],
          const SizedBox(height: LumenSpacing.lg),
          for (final advisory in advisories) ...[
            GuestAdvisoryBanner(
              key: Key('advisory-${advisory.id}'),
              advisory: advisory,
              onPressed: () => context.push('/advisories/${advisory.id}'),
              onAttractionPressed: (id) => context.push('/attractions/$id'),
            ),
            const SizedBox(height: LumenSpacing.sm),
          ],
          for (final clearedId in clearedIds) ...[
            AdvisoryClearedBanner(
              key: Key('advisory-cleared-$clearedId'),
              message: 'A park advisory was cleared.',
            ),
            const SizedBox(height: LumenSpacing.sm),
          ],
        ],
      ),
    );
  }
}
