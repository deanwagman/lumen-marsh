import 'dart:async';

import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/errors/app_failure.dart';
import '../../../../core/venue/domain/venue_stream_message.dart';
import '../../data/flow_repository.dart';
import '../../domain/guest_wait.dart';
import '../../domain/park_zone.dart';
import 'flow_event.dart';
import 'flow_state.dart';

class FlowBloc extends Bloc<FlowEvent, FlowState> {
  FlowBloc({required this.repository}) : super(const FlowInitial()) {
    on<FlowRequested>(_onRequested);
    on<FlowRefreshed>(_onRefreshed);
    on<FlowOriginZoneSelected>(_onOriginZoneSelected);
    on<FlowSnapshotReceived>(_onSnapshotReceived);
    on<FlowWaitUpdated>(_onWaitUpdated);
    on<FlowGuidancePublished>(_onGuidancePublished);
    on<FlowGuidanceWithdrawn>(_onGuidanceWithdrawn);
    on<FlowLiveUpdatesStarted>(_onLiveUpdatesStarted);
    on<FlowLiveReconnectRequested>(_onLiveReconnectRequested);
  }

  final FlowRepository repository;
  StreamSubscription<VenueStreamMessage>? _liveSubscription;

  Future<void> _onRequested(
    FlowRequested event,
    Emitter<FlowState> emit,
  ) async {
    emit(const FlowLoading());
    await _load(emit);
  }

  Future<void> _onRefreshed(
    FlowRefreshed event,
    Emitter<FlowState> emit,
  ) async {
    await _load(emit, previous: _loaded());
  }

  Future<void> _load(Emitter<FlowState> emit, {FlowLoaded? previous}) async {
    try {
      final overview = await repository.overview();
      emit(_fromOverview(overview, originZoneId: _originZoneId()));
      add(const FlowLiveUpdatesStarted());
    } on AppFailure catch (failure) {
      emit(FlowFailure(failure, previous: previous));
    } catch (_) {
      emit(const FlowFailure(UnexpectedFailure()));
    }
  }

  void _onOriginZoneSelected(
    FlowOriginZoneSelected event,
    Emitter<FlowState> emit,
  ) {
    final current = _loaded();
    if (current != null) {
      emit(current.copyWith(originZoneId: event.originZoneId));
      return;
    }
    if (state is FlowEmpty) {
      emit(
        FlowEmpty(
          originZoneId: event.originZoneId,
          updatedAt: (state as FlowEmpty).updatedAt,
        ),
      );
    }
  }

  void _onSnapshotReceived(
    FlowSnapshotReceived event,
    Emitter<FlowState> emit,
  ) {
    emit(_fromOverview(event.overview, originZoneId: _originZoneId()));
  }

  void _onWaitUpdated(FlowWaitUpdated event, Emitter<FlowState> emit) {
    final current = _loaded();
    if (current == null) {
      emit(
        FlowLoaded(
          waits: [event.wait],
          guidance: const [],
          updatedAt: event.wait.updatedAt,
          simulated: event.wait.simulated,
          originZoneId: _originZoneId(),
        ),
      );
      return;
    }
    final waits = [...current.waits];
    final index = waits.indexWhere(
      (wait) => wait.attractionId == event.wait.attractionId,
    );
    if (index == -1) {
      waits.add(event.wait);
    } else {
      final merged = waits[index].mergeLiveUpdate(event.wait);
      if (merged == waits[index]) {
        return;
      }
      waits[index] = merged;
    }
    emit(
      current.copyWith(
        waits: waits,
        updatedAt: event.wait.updatedAt,
        simulated: current.simulated || event.wait.simulated,
      ),
    );
  }

  void _onGuidancePublished(
    FlowGuidancePublished event,
    Emitter<FlowState> emit,
  ) {
    final current = _loaded();
    if (current == null) {
      emit(
        FlowLoaded(
          waits: const [],
          guidance: [event.guidance],
          updatedAt: event.guidance.updatedAt,
          simulated: event.guidance.simulated,
          originZoneId: _originZoneId(),
        ),
      );
      return;
    }
    final guidance = [
      for (final item in current.guidance)
        if (item.recommendationId != event.guidance.recommendationId) item,
      event.guidance,
    ];
    emit(
      current.copyWith(
        guidance: guidance,
        updatedAt: event.guidance.updatedAt,
        simulated: current.simulated || event.guidance.simulated,
      ),
    );
  }

  void _onGuidanceWithdrawn(
    FlowGuidanceWithdrawn event,
    Emitter<FlowState> emit,
  ) {
    final current = _loaded();
    if (current == null) {
      return;
    }
    emit(
      current.copyWith(
        guidance: [
          for (final item in current.guidance)
            if (item.recommendationId != event.recommendationId) item,
        ],
      ),
    );
  }

  Future<void> _onLiveUpdatesStarted(
    FlowLiveUpdatesStarted event,
    Emitter<FlowState> emit,
  ) async {
    await _liveSubscription?.cancel();
    _liveSubscription = repository.watchUpdates().listen((message) {
      switch (message) {
        case GuestFlowOverviewMessage(:final overview):
          add(FlowSnapshotReceived(overview));
        case GuestFlowWaitUpdatedMessage(:final wait):
          add(FlowWaitUpdated(wait));
        case GuestFlowRecommendationPublishedMessage(:final guidance):
          add(FlowGuidancePublished(guidance));
        case GuestFlowRecommendationWithdrawnMessage(:final recommendationId):
          add(FlowGuidanceWithdrawn(recommendationId));
        default:
          break;
      }
    }, onError: (_) {});
  }

  Future<void> _onLiveReconnectRequested(
    FlowLiveReconnectRequested event,
    Emitter<FlowState> emit,
  ) async {
    add(const FlowLiveUpdatesStarted());
    add(const FlowRefreshed());
  }

  FlowState _fromOverview(
    GuestFlowOverview overview, {
    required String originZoneId,
  }) {
    if (overview.attractions.isEmpty && overview.publishedGuidance.isEmpty) {
      return FlowEmpty(
        originZoneId: originZoneId,
        updatedAt: overview.updatedAt,
      );
    }
    return FlowLoaded(
      waits: overview.attractions,
      guidance: overview.publishedGuidance,
      updatedAt: overview.updatedAt,
      simulated: overview.simulated,
      originZoneId: originZoneId,
    );
  }

  FlowLoaded? _loaded() {
    return switch (state) {
      FlowLoaded() => state as FlowLoaded,
      FlowFailure(:final previous) => previous,
      _ => null,
    };
  }

  String _originZoneId() {
    return switch (state) {
      FlowLoaded(:final originZoneId) => originZoneId,
      FlowEmpty(:final originZoneId) => originZoneId,
      FlowFailure(:final previous) =>
        previous?.originZoneId ?? ParkZone.luminousWetlands.id,
      _ => ParkZone.luminousWetlands.id,
    };
  }

  @override
  Future<void> close() async {
    await _liveSubscription?.cancel();
    _liveSubscription = null;
    return super.close();
  }
}
