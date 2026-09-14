import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/errors/app_failure.dart';
import '../../../../design_system/components/feedback/lumen_feedback.dart';
import '../../../../design_system/components/layout/lumen_page.dart';
import '../../../../design_system/components/status/lumen_notice_banner.dart';
import '../../../../design_system/components/status/lumen_tone.dart';
import '../../../../design_system/foundations/lumen_spacing.dart';
import '../../../favorites/presentation/bloc/favorites_bloc.dart';
import '../../../favorites/presentation/bloc/favorites_state.dart';
import '../../domain/attraction_summary.dart';
import '../bloc/attractions_bloc.dart';
import '../bloc/attractions_event.dart';
import '../bloc/attractions_state.dart';
import '../widgets/attraction_card.dart';
import '../widgets/live_connection_banner.dart';

class AttractionsPage extends StatelessWidget {
  const AttractionsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return LumenPage(
      title: 'Attractions',
      actions: [
        IconButton(
          tooltip: 'Refresh attractions',
          onPressed: () =>
              context.read<AttractionsBloc>().add(const AttractionsRefreshed()),
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
              message: 'The catalog is quiet right now. Refresh when VenueOps has listings.',
              onRetry: () => context.read<AttractionsBloc>().add(
                const AttractionsRequested(),
              ),
            ),
            AttractionsLoaded() ||
            AttractionsRefreshing() => _AttractionsList(state: state),
            AttractionsFailure(:final failure, :final previous) =>
              previous != null && previous.isNotEmpty
                  ? _AttractionsList(
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

class _AttractionsList extends StatelessWidget {
  const _AttractionsList({required this.state, this.bannerMessage});

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

        return RefreshIndicator(
          onRefresh: () {
            final bloc = context.read<AttractionsBloc>();
            bloc.add(const AttractionsRefreshed());
            return bloc.stream.firstWhere(
              (state) =>
                  state is AttractionsLoaded ||
                  state is AttractionsEmpty ||
                  state is AttractionsFailure,
            );
          },
          child: ListView(
            key: const Key('attractions-list'),
            physics: const AlwaysScrollableScrollPhysics(),
            padding: LumenSpacing.pageInsets,
            children: [
              Text(
                'Live operational conditions from VenueOps.',
                style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: LumenSpacing.md),
              LiveConnectionBanner(state: state),
              if (bannerMessage != null) ...[
                const SizedBox(height: LumenSpacing.md),
                LumenNoticeBanner(
                  tone: LumenTone.critical,
                  message: bannerMessage!,
                ),
              ],
              const SizedBox(height: LumenSpacing.md),
              for (var i = 0; i < attractions.length; i++) ...[
                AttractionCard(
                  attraction: attractions[i],
                  isFavorite: favoriteIds.contains(attractions[i].id),
                ),
                if (i != attractions.length - 1)
                  const SizedBox(height: LumenSpacing.sm),
              ],
            ],
          ),
        );
      },
    );
  }
}
