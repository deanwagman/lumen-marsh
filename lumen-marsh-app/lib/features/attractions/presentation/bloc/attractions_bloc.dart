import 'dart:async';

import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/errors/app_failure.dart';
import '../../data/attraction_repository.dart';
import '../../domain/attraction_live.dart';
import '../../../../core/venue/domain/venue_stream_message.dart';
import '../../domain/attraction_summary.dart';
import 'attraction_catalog_merge.dart';
import 'attractions_event.dart';
import 'attractions_state.dart';

class AttractionsBloc extends Bloc<AttractionsEvent, AttractionsState> {
  AttractionsBloc({required this.repository})
    : super(const AttractionsInitial()) {
    on<AttractionsRequested>(_onRequested);
    on<AttractionsRefreshed>(_onRefreshed);
    on<AttractionsLiveUpdatesStarted>(_onLiveUpdatesStarted);
    on<AttractionsSnapshotReceived>(_onSnapshotReceived);
    on<AttractionLiveUpdateReceived>(_onLiveUpdateReceived);
    on<AttractionsConnectionChanged>(_onConnectionChanged);
    on<AttractionsLiveReconnectRequested>(_onLiveReconnectRequested);
  }

  final AttractionRepository repository;
  StreamSubscription<VenueStreamMessage>? _liveSubscription;

  Future<void> _onRequested(
    AttractionsRequested event,
    Emitter<AttractionsState> emit,
  ) async {
    emit(const AttractionsLoading());
    await _load(emit);
  }

  Future<void> _onRefreshed(
    AttractionsRefreshed event,
    Emitter<AttractionsState> emit,
  ) async {
    final previous = _currentAttractions();
    final connection = _connectionFromState();
    if (previous != null) {
      emit(
        AttractionsRefreshing(
          previous,
          connectionStatus: connection.status,
          lastSyncedAt: connection.lastSyncedAt,
          connectionMessage: connection.message,
        ),
      );
    } else {
      emit(const AttractionsLoading());
    }
    await _load(emit, previous: previous);
  }

  Future<void> _load(
    Emitter<AttractionsState> emit, {
    List<AttractionSummary>? previous,
  }) async {
    try {
      final attractions = await repository.list();
      if (attractions.isEmpty) {
        emit(const AttractionsEmpty());
      } else {
        emit(
          AttractionsLoaded(
            attractions,
            connectionStatus: LiveConnectionStatus.connecting,
            lastSyncedAt: DateTime.now().toUtc(),
          ),
        );
        add(const AttractionsLiveUpdatesStarted());
      }
    } on AppFailure catch (failure) {
      emit(AttractionsFailure(failure, previous: previous));
    } catch (_) {
      emit(const AttractionsFailure(UnexpectedFailure()));
    }
  }

  Future<void> _onLiveUpdatesStarted(
    AttractionsLiveUpdatesStarted event,
    Emitter<AttractionsState> emit,
  ) async {
    await _liveSubscription?.cancel();
    _emitConnection(emit, LiveConnectionStatus.connecting);
    _liveSubscription = repository.watchUpdates().listen(
      (message) {
        switch (message) {
          case AttractionsSnapshotMessage(:final attractions):
            add(AttractionsSnapshotReceived(attractions));
          case AttractionUpdatedMessage(:final update):
            add(AttractionLiveUpdateReceived(update));
          default:
            break;
        }
      },
      onError: (_) {
        add(
          const AttractionsConnectionChanged(
            LiveConnectionStatus.stale,
            message: 'Live updates paused—showing last known conditions',
          ),
        );
      },
    );
  }

  void _onSnapshotReceived(
    AttractionsSnapshotReceived event,
    Emitter<AttractionsState> emit,
  ) {
    final current = _currentAttractions();
    if (current == null) {
      if (event.attractions.isEmpty) {
        emit(const AttractionsEmpty());
      } else {
        emit(
          AttractionsLoaded(
            event.attractions,
            connectionStatus: LiveConnectionStatus.connected,
            lastSyncedAt: DateTime.now().toUtc(),
          ),
        );
      }
      return;
    }

    final merged = mergeAttractionsSnapshot(
      current: current,
      snapshot: event.attractions,
    );
    emit(
      AttractionsLoaded(
        merged,
        connectionStatus: LiveConnectionStatus.connected,
        lastSyncedAt: DateTime.now().toUtc(),
      ),
    );
  }

  Future<void> _onLiveUpdateReceived(
    AttractionLiveUpdateReceived event,
    Emitter<AttractionsState> emit,
  ) async {
    final current = _currentAttractions();
    if (current == null) {
      return;
    }

    final next = applyAttractionUpdate(current: current, update: event.update);
    if (next == null) {
      add(const AttractionsRefreshed());
      return;
    }
    if (identical(next, current)) {
      return;
    }

    emit(
      AttractionsLoaded(
        next,
        connectionStatus: LiveConnectionStatus.connected,
        lastSyncedAt: DateTime.now().toUtc(),
      ),
    );
  }

  void _onConnectionChanged(
    AttractionsConnectionChanged event,
    Emitter<AttractionsState> emit,
  ) {
    _emitConnection(emit, event.status, message: event.message);
  }

  Future<void> _onLiveReconnectRequested(
    AttractionsLiveReconnectRequested event,
    Emitter<AttractionsState> emit,
  ) async {
    _emitConnection(
      emit,
      LiveConnectionStatus.reconnecting,
      message: 'Reconnecting to live park conditions…',
    );
    add(const AttractionsLiveUpdatesStarted());
  }

  void _emitConnection(
    Emitter<AttractionsState> emit,
    LiveConnectionStatus status, {
    String? message,
  }) {
    final current = state;
    if (current is AttractionsLoaded) {
      emit(
        current.copyWith(
          connectionStatus: status,
          connectionMessage: message,
          clearConnectionMessage: message == null,
        ),
      );
    } else if (current is AttractionsRefreshing) {
      emit(
        AttractionsRefreshing(
          current.attractions,
          connectionStatus: status,
          lastSyncedAt: current.lastSyncedAt,
          connectionMessage: message,
        ),
      );
    }
  }

  ({LiveConnectionStatus status, DateTime? lastSyncedAt, String? message})
  _connectionFromState() {
    return switch (state) {
      AttractionsLoaded(
        :final connectionStatus,
        :final lastSyncedAt,
        :final connectionMessage,
      ) =>
        (
          status: connectionStatus,
          lastSyncedAt: lastSyncedAt,
          message: connectionMessage,
        ),
      AttractionsRefreshing(
        :final connectionStatus,
        :final lastSyncedAt,
        :final connectionMessage,
      ) =>
        (
          status: connectionStatus,
          lastSyncedAt: lastSyncedAt,
          message: connectionMessage,
        ),
      _ => (
        status: LiveConnectionStatus.connecting,
        lastSyncedAt: null,
        message: null,
      ),
    };
  }

  List<AttractionSummary>? _currentAttractions() {
    return switch (state) {
      AttractionsLoaded(:final attractions) => attractions,
      AttractionsRefreshing(:final attractions) => attractions,
      AttractionsFailure(:final previous) => previous,
      _ => null,
    };
  }

  @override
  Future<void> close() async {
    await _liveSubscription?.cancel();
    _liveSubscription = null;
    return super.close();
  }
}
