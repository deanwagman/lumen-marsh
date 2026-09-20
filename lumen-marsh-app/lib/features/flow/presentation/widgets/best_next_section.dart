import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../design_system/components/layout/lumen_page.dart';
import '../../../../design_system/components/status/lumen_notice_banner.dart';
import '../../../../design_system/components/status/lumen_tone.dart';
import '../../../../design_system/foundations/lumen_spacing.dart';
import '../../../attractions/domain/attraction_summary.dart';
import '../../domain/best_next_ranker.dart';
import '../../domain/park_zone.dart';
import '../bloc/flow_bloc.dart';
import '../bloc/flow_event.dart';
import '../bloc/flow_state.dart';
import '../flow_scope.dart';
import 'best_next_card.dart';

class BestNextSection extends StatefulWidget {
  const BestNextSection({
    super.key,
    required this.catalog,
    required this.favoriteIds,
    required this.onAttractionPressed,
  });

  final List<AttractionSummary> catalog;
  final Set<String> favoriteIds;
  final void Function(String attractionId) onAttractionPressed;

  @override
  State<BestNextSection> createState() => _BestNextSectionState();
}

class _BestNextSectionState extends State<BestNextSection> {
  List<String> _previousOrder = const [];

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

        final ranked = const BestNextRanker().rank(
          waits: state.waits,
          originZoneId: state.originZoneId,
          catalog: widget.catalog,
          favoriteIds: widget.favoriteIds,
          publishedGuidance: state.guidance,
          previousOrder: _previousOrder,
        );
        final nextOrder = [for (final item in ranked) item.attractionId];
        if (!_ordersEqual(_previousOrder, nextOrder) &&
            !ranked.any((item) => item.wait.isStale)) {
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (mounted) {
              setState(() => _previousOrder = nextOrder);
            }
          });
        }

        if (ranked.isEmpty && state.guidance.isEmpty) {
          return const SizedBox.shrink();
        }

        final stale = state.hasStaleWaits;

        return Padding(
          padding: const EdgeInsets.only(top: LumenSpacing.lg),
          child: LumenSection(
            title: 'Best Next Experiences',
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  'Choose where you are now. Suggestions use posted waits, a 30-minute outlook, and published park guidance.',
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                ),
                const SizedBox(height: LumenSpacing.sm),
                _OriginZonePicker(
                  originZoneId: state.originZoneId,
                  onSelected: (zoneId) => context.read<FlowBloc>().add(
                    FlowOriginZoneSelected(zoneId),
                  ),
                ),
                if (state.simulated) ...[
                  const SizedBox(height: LumenSpacing.sm),
                  const LumenNoticeBanner(
                    tone: LumenTone.informational,
                    message: 'Simulated demonstration data',
                  ),
                ],
                if (stale) ...[
                  const SizedBox(height: LumenSpacing.sm),
                  const LumenNoticeBanner(
                    tone: LumenTone.warning,
                    title: 'Stale wait data',
                    message: 'Queue observations are older than five minutes. Rankings stay frozen until fresh telemetry arrives.',
                  ),
                ],
                if (state.guidance.isNotEmpty) ...[
                  const SizedBox(height: LumenSpacing.md),
                  for (final item in state.guidance) ...[
                    LumenNoticeBanner(
                      tone: LumenTone.informational,
                      title: 'Published guidance',
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
                if (ranked.isNotEmpty) ...[
                  const SizedBox(height: LumenSpacing.md),
                  _RecommendationCards(
                    ranked: ranked,
                    originZoneId: state.originZoneId,
                    onAttractionPressed: widget.onAttractionPressed,
                  ),
                ],
              ],
            ),
          ),
        );
      },
    );
  }

  bool _ordersEqual(List<String> left, List<String> right) {
    if (left.length != right.length) {
      return false;
    }
    for (var i = 0; i < left.length; i++) {
      if (left[i] != right[i]) {
        return false;
      }
    }
    return true;
  }
}

class _OriginZonePicker extends StatelessWidget {
  const _OriginZonePicker({
    required this.originZoneId,
    required this.onSelected,
  });

  final String originZoneId;
  final ValueChanged<String> onSelected;

  @override
  Widget build(BuildContext context) {
    return KeyedSubtree(
      key: const Key('best-next-origin-zone'),
      child: DropdownButtonFormField<String>(
        key: ValueKey(originZoneId),
        initialValue: originZoneId,
        decoration: const InputDecoration(
          labelText: 'Starting zone',
          border: OutlineInputBorder(),
        ),
        items: [
          for (final zone in ParkZone.all)
            DropdownMenuItem(value: zone.id, child: Text(zone.name)),
        ],
        onChanged: (value) {
          if (value != null) {
            onSelected(value);
          }
        },
      ),
    );
  }
}

class _RecommendationCards extends StatelessWidget {
  const _RecommendationCards({
    required this.ranked,
    required this.originZoneId,
    required this.onAttractionPressed,
  });

  final List<BestNextRecommendation> ranked;
  final String originZoneId;
  final void Function(String attractionId) onAttractionPressed;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        for (var i = 0; i < ranked.length; i++) ...[
          BestNextCard(
            key: Key('best-next-${ranked[i].attractionId}'),
            recommendation: ranked[i],
            originZoneId: originZoneId,
            onPressed: () => onAttractionPressed(ranked[i].attractionId),
          ),
          if (i != ranked.length - 1) const SizedBox(height: LumenSpacing.sm),
        ],
      ],
    );
  }
}
