import 'package:equatable/equatable.dart';

import '../../../../core/errors/app_failure.dart';
import '../../domain/attraction_live.dart';
import '../../domain/attraction_summary.dart';

sealed class AttractionsState extends Equatable {
  const AttractionsState();

  @override
  List<Object?> get props => [];
}

final class AttractionsInitial extends AttractionsState {
  const AttractionsInitial();
}

final class AttractionsLoading extends AttractionsState {
  const AttractionsLoading();
}

final class AttractionsLoaded extends AttractionsState {
  const AttractionsLoaded(
    this.attractions, {
    this.connectionStatus = LiveConnectionStatus.connecting,
    this.lastSyncedAt,
    this.connectionMessage,
  });

  final List<AttractionSummary> attractions;
  final LiveConnectionStatus connectionStatus;
  final DateTime? lastSyncedAt;
  final String? connectionMessage;

  AttractionsLoaded copyWith({
    List<AttractionSummary>? attractions,
    LiveConnectionStatus? connectionStatus,
    DateTime? lastSyncedAt,
    String? connectionMessage,
    bool clearConnectionMessage = false,
  }) {
    return AttractionsLoaded(
      attractions ?? this.attractions,
      connectionStatus: connectionStatus ?? this.connectionStatus,
      lastSyncedAt: lastSyncedAt ?? this.lastSyncedAt,
      connectionMessage: clearConnectionMessage
          ? null
          : (connectionMessage ?? this.connectionMessage),
    );
  }

  @override
  List<Object?> get props => [
    attractions,
    connectionStatus,
    lastSyncedAt,
    connectionMessage,
  ];
}

final class AttractionsRefreshing extends AttractionsState {
  const AttractionsRefreshing(
    this.attractions, {
    this.connectionStatus = LiveConnectionStatus.connecting,
    this.lastSyncedAt,
    this.connectionMessage,
  });

  final List<AttractionSummary> attractions;
  final LiveConnectionStatus connectionStatus;
  final DateTime? lastSyncedAt;
  final String? connectionMessage;

  @override
  List<Object?> get props => [
    attractions,
    connectionStatus,
    lastSyncedAt,
    connectionMessage,
  ];
}

final class AttractionsEmpty extends AttractionsState {
  const AttractionsEmpty();
}

final class AttractionsFailure extends AttractionsState {
  const AttractionsFailure(this.failure, {this.previous});

  final AppFailure failure;
  final List<AttractionSummary>? previous;

  @override
  List<Object?> get props => [failure, previous];
}
