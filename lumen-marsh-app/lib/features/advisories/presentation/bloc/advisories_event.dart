import 'package:equatable/equatable.dart';

import '../../../../core/venue/domain/venue_live.dart';
import '../../domain/guest_advisory.dart';

sealed class AdvisoriesEvent extends Equatable {
  const AdvisoriesEvent();

  @override
  List<Object?> get props => [];
}

final class AdvisoriesRequested extends AdvisoriesEvent {
  const AdvisoriesRequested();
}

final class AdvisoriesRefreshed extends AdvisoriesEvent {
  const AdvisoriesRefreshed();
}

final class AdvisoriesSnapshotReceived extends AdvisoriesEvent {
  const AdvisoriesSnapshotReceived(this.advisories);

  final List<GuestAdvisory> advisories;

  @override
  List<Object?> get props => [advisories];
}

final class AdvisoryUpdated extends AdvisoriesEvent {
  const AdvisoryUpdated(this.advisory);

  final GuestAdvisory advisory;

  @override
  List<Object?> get props => [advisory];
}

final class AdvisoryWithdrawn extends AdvisoriesEvent {
  const AdvisoryWithdrawn(this.advisoryId, this.version);

  final String advisoryId;
  final int version;

  @override
  List<Object?> get props => [advisoryId, version];
}

final class AdvisoriesConnectionChanged extends AdvisoriesEvent {
  const AdvisoriesConnectionChanged(this.status, {this.message});

  final LiveConnectionStatus status;
  final String? message;

  @override
  List<Object?> get props => [status, message];
}

final class AdvisoriesLiveUpdatesStarted extends AdvisoriesEvent {
  const AdvisoriesLiveUpdatesStarted();
}

final class AdvisoriesLiveReconnectRequested extends AdvisoriesEvent {
  const AdvisoriesLiveReconnectRequested();
}
