import 'package:equatable/equatable.dart';

import '../../domain/attraction_operational_update.dart';
import '../../domain/attraction_summary.dart';
import '../../domain/attraction_live.dart';

sealed class AttractionsEvent extends Equatable {
  const AttractionsEvent();

  @override
  List<Object?> get props => [];
}

final class AttractionsRequested extends AttractionsEvent {
  const AttractionsRequested();
}

final class AttractionsRefreshed extends AttractionsEvent {
  const AttractionsRefreshed();
}

final class AttractionsLiveUpdatesStarted extends AttractionsEvent {
  const AttractionsLiveUpdatesStarted();
}

final class AttractionsSnapshotReceived extends AttractionsEvent {
  const AttractionsSnapshotReceived(this.attractions);

  final List<AttractionSummary> attractions;

  @override
  List<Object?> get props => [attractions];
}

final class AttractionLiveUpdateReceived extends AttractionsEvent {
  const AttractionLiveUpdateReceived(this.update);

  final AttractionOperationalUpdate update;

  @override
  List<Object?> get props => [update];
}

final class AttractionsConnectionChanged extends AttractionsEvent {
  const AttractionsConnectionChanged(this.status, {this.message});

  final LiveConnectionStatus status;
  final String? message;

  @override
  List<Object?> get props => [status, message];
}

final class AttractionsLiveReconnectRequested extends AttractionsEvent {
  const AttractionsLiveReconnectRequested();
}
