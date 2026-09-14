import 'package:equatable/equatable.dart';

import '../../../../core/errors/app_failure.dart';
import '../../../../core/venue/domain/venue_live.dart';
import '../../domain/guest_advisory.dart';

sealed class AdvisoriesState extends Equatable {
  const AdvisoriesState();

  @override
  List<Object?> get props => [];
}

final class AdvisoriesInitial extends AdvisoriesState {
  const AdvisoriesInitial();
}

final class AdvisoriesLoading extends AdvisoriesState {
  const AdvisoriesLoading();
}

final class AdvisoriesLoaded extends AdvisoriesState {
  const AdvisoriesLoaded(
    this.advisories, {
    this.connectionStatus = LiveConnectionStatus.connecting,
    this.lastSyncedAt,
    this.connectionMessage,
    this.clearedAdvisoryIds = const {},
  });

  final List<GuestAdvisory> advisories;
  final LiveConnectionStatus connectionStatus;
  final DateTime? lastSyncedAt;
  final String? connectionMessage;
  final Set<String> clearedAdvisoryIds;

  AdvisoriesLoaded copyWith({
    List<GuestAdvisory>? advisories,
    LiveConnectionStatus? connectionStatus,
    DateTime? lastSyncedAt,
    String? connectionMessage,
    Set<String>? clearedAdvisoryIds,
    bool clearConnectionMessage = false,
  }) {
    return AdvisoriesLoaded(
      advisories ?? this.advisories,
      connectionStatus: connectionStatus ?? this.connectionStatus,
      lastSyncedAt: lastSyncedAt ?? this.lastSyncedAt,
      connectionMessage: clearConnectionMessage
          ? null
          : (connectionMessage ?? this.connectionMessage),
      clearedAdvisoryIds: clearedAdvisoryIds ?? this.clearedAdvisoryIds,
    );
  }

  @override
  List<Object?> get props => [
    advisories,
    connectionStatus,
    lastSyncedAt,
    connectionMessage,
    clearedAdvisoryIds,
  ];
}

final class AdvisoriesRefreshing extends AdvisoriesState {
  const AdvisoriesRefreshing(
    this.advisories, {
    this.connectionStatus = LiveConnectionStatus.connecting,
    this.lastSyncedAt,
    this.connectionMessage,
    this.clearedAdvisoryIds = const {},
  });

  final List<GuestAdvisory> advisories;
  final LiveConnectionStatus connectionStatus;
  final DateTime? lastSyncedAt;
  final String? connectionMessage;
  final Set<String> clearedAdvisoryIds;

  @override
  List<Object?> get props => [
    advisories,
    connectionStatus,
    lastSyncedAt,
    connectionMessage,
    clearedAdvisoryIds,
  ];
}

final class AdvisoriesEmpty extends AdvisoriesState {
  const AdvisoriesEmpty({
    this.connectionStatus = LiveConnectionStatus.connected,
    this.lastSyncedAt,
  });

  final LiveConnectionStatus connectionStatus;
  final DateTime? lastSyncedAt;

  @override
  List<Object?> get props => [connectionStatus, lastSyncedAt];
}

final class AdvisoriesFailure extends AdvisoriesState {
  const AdvisoriesFailure(this.failure, {this.previous});

  final AppFailure failure;
  final List<GuestAdvisory>? previous;

  @override
  List<Object?> get props => [failure, previous];
}
