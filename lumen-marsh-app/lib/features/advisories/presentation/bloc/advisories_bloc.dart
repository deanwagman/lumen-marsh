import 'dart:async';

import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/errors/app_failure.dart';
import '../../../../core/venue/domain/venue_live.dart';
import '../../../../core/venue/domain/venue_stream_message.dart';
import '../../data/advisory_repository.dart';
import '../../domain/guest_advisory.dart';
import 'advisories_event.dart';
import 'advisories_state.dart';
import 'advisory_catalog_merge.dart';

class AdvisoriesBloc extends Bloc<AdvisoriesEvent, AdvisoriesState> {
  AdvisoriesBloc({required this.repository})
    : _withdrawnVersions = {},
      super(const AdvisoriesInitial()) {
    on<AdvisoriesRequested>(_onRequested);
    on<AdvisoriesRefreshed>(_onRefreshed);
    on<AdvisoriesLiveUpdatesStarted>(_onLiveUpdatesStarted);
    on<AdvisoriesSnapshotReceived>(_onSnapshotReceived);
    on<AdvisoryUpdated>(_onAdvisoryUpdated);
    on<AdvisoryWithdrawn>(_onAdvisoryWithdrawn);
    on<AdvisoriesConnectionChanged>(_onConnectionChanged);
    on<AdvisoriesLiveReconnectRequested>(_onLiveReconnectRequested);
  }

  final AdvisoryRepository repository;
  final Map<String, int> _withdrawnVersions;
  StreamSubscription<VenueStreamMessage>? _liveSubscription;
  StreamSubscription<LiveConnectionStatus>? _connectionSubscription;

  Future<void> _onRequested(
    AdvisoriesRequested event,
    Emitter<AdvisoriesState> emit,
  ) async {
    emit(const AdvisoriesLoading());
    await _load(emit);
  }

  Future<void> _onRefreshed(
    AdvisoriesRefreshed event,
    Emitter<AdvisoriesState> emit,
  ) async {
    final previous = _currentAdvisories();
    final connection = _connectionFromState();
    if (previous != null) {
      emit(
        AdvisoriesRefreshing(
          previous,
          connectionStatus: connection.status,
          lastSyncedAt: connection.lastSyncedAt,
          connectionMessage: connection.message,
          clearedAdvisoryIds: _clearedFromState(),
        ),
      );
    } else {
      emit(const AdvisoriesLoading());
    }
    await _load(emit, previous: previous);
  }

  Future<void> _load(
    Emitter<AdvisoriesState> emit, {
    List<GuestAdvisory>? previous,
  }) async {
    try {
      final advisories = sortAdvisories(await repository.listActive());
      if (advisories.isEmpty) {
        emit(
          AdvisoriesEmpty(
            connectionStatus: LiveConnectionStatus.connecting,
            lastSyncedAt: DateTime.now().toUtc(),
          ),
        );
      } else {
        emit(
          AdvisoriesLoaded(
            advisories,
            connectionStatus: LiveConnectionStatus.connecting,
            lastSyncedAt: DateTime.now().toUtc(),
          ),
        );
      }
      add(const AdvisoriesLiveUpdatesStarted());
    } on AppFailure catch (failure) {
      emit(AdvisoriesFailure(failure, previous: previous));
    } catch (_) {
      emit(const AdvisoriesFailure(UnexpectedFailure()));
    }
  }

  Future<void> _onLiveUpdatesStarted(
    AdvisoriesLiveUpdatesStarted event,
    Emitter<AdvisoriesState> emit,
  ) async {
    await _liveSubscription?.cancel();
    await _connectionSubscription?.cancel();
    _emitConnection(emit, LiveConnectionStatus.connecting);
    _connectionSubscription = repository.watchConnectionStatus().listen(
      (status) => add(AdvisoriesConnectionChanged(status)),
    );
    _liveSubscription = repository.watchUpdates().listen(
      (message) {
        switch (message) {
          case AdvisoriesSnapshotMessage(:final advisories):
            add(AdvisoriesSnapshotReceived(advisories));
          case AdvisoryPublishedMessage(:final advisory):
          case AdvisoryUpdatedMessage(:final advisory):
            add(AdvisoryUpdated(advisory));
          case AdvisoryWithdrawnMessage(:final advisoryId, :final version):
            add(AdvisoryWithdrawn(advisoryId, version));
          default:
            break;
        }
      },
      onError: (_) {
        add(
          const AdvisoriesConnectionChanged(
            LiveConnectionStatus.stale,
            message: 'Live updates paused—showing last known advisories',
          ),
        );
      },
    );
  }

  void _onSnapshotReceived(
    AdvisoriesSnapshotReceived event,
    Emitter<AdvisoriesState> emit,
  ) {
    final current = _currentAdvisories() ?? const <GuestAdvisory>[];
    final merged = mergeAdvisoriesSnapshot(
      current: current,
      snapshot: event.advisories,
      withdrawnVersions: _withdrawnVersions,
    );
    if (merged.isEmpty) {
      emit(
        AdvisoriesEmpty(
          connectionStatus: LiveConnectionStatus.connected,
          lastSyncedAt: DateTime.now().toUtc(),
        ),
      );
      return;
    }
    emit(
      AdvisoriesLoaded(
        merged,
        connectionStatus: LiveConnectionStatus.connected,
        lastSyncedAt: DateTime.now().toUtc(),
      ),
    );
  }

  void _onAdvisoryUpdated(
    AdvisoryUpdated event,
    Emitter<AdvisoriesState> emit,
  ) {
    final current = _currentAdvisories();
    if (current == null) {
      return;
    }

    final next = applyAdvisoryLiveUpdate(
      current: current,
      update: event.advisory,
      withdrawnVersions: _withdrawnVersions,
    );
    if (next == null || identical(next, current)) {
      return;
    }

    emit(
      AdvisoriesLoaded(
        next,
        connectionStatus: LiveConnectionStatus.connected,
        lastSyncedAt: DateTime.now().toUtc(),
        clearedAdvisoryIds: _withoutCleared(event.advisory.id),
      ),
    );
  }

  void _onAdvisoryWithdrawn(
    AdvisoryWithdrawn event,
    Emitter<AdvisoriesState> emit,
  ) {
    final current = _currentAdvisories();
    if (current == null) {
      return;
    }

    final next = applyAdvisoryLiveWithdrawn(
      current: current,
      advisoryId: event.advisoryId,
      version: event.version,
      withdrawnVersions: _withdrawnVersions,
    );
    if (next.isEmpty) {
      emit(
        AdvisoriesEmpty(
          connectionStatus: LiveConnectionStatus.connected,
          lastSyncedAt: DateTime.now().toUtc(),
        ),
      );
      return;
    }

    emit(
      AdvisoriesLoaded(
        next,
        connectionStatus: LiveConnectionStatus.connected,
        lastSyncedAt: DateTime.now().toUtc(),
        clearedAdvisoryIds: {..._clearedFromState(), event.advisoryId},
      ),
    );
  }

  void _onConnectionChanged(
    AdvisoriesConnectionChanged event,
    Emitter<AdvisoriesState> emit,
  ) {
    _emitConnection(emit, event.status, message: event.message);
  }

  Future<void> _onLiveReconnectRequested(
    AdvisoriesLiveReconnectRequested event,
    Emitter<AdvisoriesState> emit,
  ) async {
    _emitConnection(
      emit,
      LiveConnectionStatus.reconnecting,
      message: 'Reconnecting to live park conditions…',
    );
    add(const AdvisoriesLiveUpdatesStarted());
    add(const AdvisoriesRefreshed());
  }

  void _emitConnection(
    Emitter<AdvisoriesState> emit,
    LiveConnectionStatus status, {
    String? message,
  }) {
    final current = state;
    if (current is AdvisoriesLoaded) {
      emit(
        current.copyWith(
          connectionStatus: status,
          connectionMessage: message,
          clearConnectionMessage: message == null,
        ),
      );
    } else if (current is AdvisoriesRefreshing) {
      emit(
        AdvisoriesRefreshing(
          current.advisories,
          connectionStatus: status,
          lastSyncedAt: current.lastSyncedAt,
          connectionMessage: message,
          clearedAdvisoryIds: current.clearedAdvisoryIds,
        ),
      );
    } else if (current is AdvisoriesEmpty) {
      emit(
        AdvisoriesEmpty(
          connectionStatus: status,
          lastSyncedAt: current.lastSyncedAt,
        ),
      );
    }
  }

  Set<String> _clearedFromState() {
    return switch (state) {
      AdvisoriesLoaded(:final clearedAdvisoryIds) => clearedAdvisoryIds,
      AdvisoriesRefreshing(:final clearedAdvisoryIds) => clearedAdvisoryIds,
      _ => const {},
    };
  }

  Set<String> _withoutCleared(String advisoryId) {
    final cleared = {..._clearedFromState()};
    cleared.remove(advisoryId);
    return cleared;
  }

  ({LiveConnectionStatus status, DateTime? lastSyncedAt, String? message})
  _connectionFromState() {
    return switch (state) {
      AdvisoriesLoaded(
        :final connectionStatus,
        :final lastSyncedAt,
        :final connectionMessage,
      ) =>
        (
          status: connectionStatus,
          lastSyncedAt: lastSyncedAt,
          message: connectionMessage,
        ),
      AdvisoriesRefreshing(
        :final connectionStatus,
        :final lastSyncedAt,
        :final connectionMessage,
      ) =>
        (
          status: connectionStatus,
          lastSyncedAt: lastSyncedAt,
          message: connectionMessage,
        ),
      AdvisoriesEmpty(:final connectionStatus, :final lastSyncedAt) => (
        status: connectionStatus,
        lastSyncedAt: lastSyncedAt,
        message: null,
      ),
      _ => (
        status: LiveConnectionStatus.connecting,
        lastSyncedAt: null,
        message: null,
      ),
    };
  }

  List<GuestAdvisory>? _currentAdvisories() {
    return switch (state) {
      AdvisoriesLoaded(:final advisories) => advisories,
      AdvisoriesRefreshing(:final advisories) => advisories,
      AdvisoriesFailure(:final previous) => previous,
      AdvisoriesEmpty() => const [],
      _ => null,
    };
  }

  @override
  Future<void> close() async {
    await _liveSubscription?.cancel();
    await _connectionSubscription?.cancel();
    _liveSubscription = null;
    _connectionSubscription = null;
    return super.close();
  }
}
