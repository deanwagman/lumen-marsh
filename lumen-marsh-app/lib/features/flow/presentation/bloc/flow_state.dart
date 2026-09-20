import 'package:equatable/equatable.dart';

import '../../../../core/errors/app_failure.dart';
import '../../domain/guest_wait.dart';
import '../../domain/park_zone.dart';

sealed class FlowState extends Equatable {
  const FlowState();

  @override
  List<Object?> get props => [];
}

final class FlowInitial extends FlowState {
  const FlowInitial();
}

final class FlowLoading extends FlowState {
  const FlowLoading();
}

final class FlowLoaded extends FlowState {
  const FlowLoaded({
    required this.waits,
    required this.guidance,
    required this.updatedAt,
    required this.simulated,
    this.originZoneId = ParkZone.luminousWetlandsId,
  });

  final List<GuestWait> waits;
  final List<GuestGuidance> guidance;
  final DateTime updatedAt;
  final bool simulated;
  final String originZoneId;

  GuestWait? waitFor(String attractionId) {
    for (final wait in waits) {
      if (wait.attractionId == attractionId) {
        return wait;
      }
    }
    return null;
  }

  bool get hasStaleWaits => waits.any((wait) => wait.isStale);

  FlowLoaded copyWith({
    List<GuestWait>? waits,
    List<GuestGuidance>? guidance,
    DateTime? updatedAt,
    bool? simulated,
    String? originZoneId,
  }) {
    return FlowLoaded(
      waits: waits ?? this.waits,
      guidance: guidance ?? this.guidance,
      updatedAt: updatedAt ?? this.updatedAt,
      simulated: simulated ?? this.simulated,
      originZoneId: originZoneId ?? this.originZoneId,
    );
  }

  @override
  List<Object?> get props => [
    waits,
    guidance,
    updatedAt,
    simulated,
    originZoneId,
  ];
}

final class FlowEmpty extends FlowState {
  const FlowEmpty({
    this.originZoneId = ParkZone.luminousWetlandsId,
    this.updatedAt,
  });

  final String originZoneId;
  final DateTime? updatedAt;

  @override
  List<Object?> get props => [originZoneId, updatedAt];
}

final class FlowFailure extends FlowState {
  const FlowFailure(this.failure, {this.previous});

  final AppFailure failure;
  final FlowLoaded? previous;

  @override
  List<Object?> get props => [failure, previous];
}
