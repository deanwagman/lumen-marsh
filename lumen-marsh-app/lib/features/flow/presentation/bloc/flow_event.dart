import 'package:equatable/equatable.dart';

import '../../domain/guest_wait.dart';

sealed class FlowEvent extends Equatable {
  const FlowEvent();

  @override
  List<Object?> get props => [];
}

final class FlowRequested extends FlowEvent {
  const FlowRequested();
}

final class FlowRefreshed extends FlowEvent {
  const FlowRefreshed();
}

final class FlowOriginZoneSelected extends FlowEvent {
  const FlowOriginZoneSelected(this.originZoneId);

  final String originZoneId;

  @override
  List<Object?> get props => [originZoneId];
}

final class FlowSnapshotReceived extends FlowEvent {
  const FlowSnapshotReceived(this.overview);

  final GuestFlowOverview overview;

  @override
  List<Object?> get props => [overview];
}

final class FlowWaitUpdated extends FlowEvent {
  const FlowWaitUpdated(this.wait);

  final GuestWait wait;

  @override
  List<Object?> get props => [wait];
}

final class FlowGuidancePublished extends FlowEvent {
  const FlowGuidancePublished(this.guidance);

  final GuestGuidance guidance;

  @override
  List<Object?> get props => [guidance];
}

final class FlowGuidanceWithdrawn extends FlowEvent {
  const FlowGuidanceWithdrawn(this.recommendationId);

  final String recommendationId;

  @override
  List<Object?> get props => [recommendationId];
}

final class FlowLiveUpdatesStarted extends FlowEvent {
  const FlowLiveUpdatesStarted();
}

final class FlowLiveReconnectRequested extends FlowEvent {
  const FlowLiveReconnectRequested();
}
