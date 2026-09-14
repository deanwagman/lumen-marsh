import 'package:equatable/equatable.dart';

import '../../../features/advisories/domain/guest_advisory.dart';
import '../../../features/attractions/domain/attraction_operational_update.dart';
import '../../../features/attractions/domain/attraction_summary.dart';

sealed class VenueStreamMessage extends Equatable {
  const VenueStreamMessage();

  @override
  List<Object?> get props => [];
}

final class AttractionsSnapshotMessage extends VenueStreamMessage {
  const AttractionsSnapshotMessage(this.attractions);

  final List<AttractionSummary> attractions;

  @override
  List<Object?> get props => [attractions];
}

final class AttractionUpdatedMessage extends VenueStreamMessage {
  const AttractionUpdatedMessage(this.update);

  final AttractionOperationalUpdate update;

  @override
  List<Object?> get props => [update];
}

final class AdvisoriesSnapshotMessage extends VenueStreamMessage {
  const AdvisoriesSnapshotMessage(this.advisories);

  final List<GuestAdvisory> advisories;

  @override
  List<Object?> get props => [advisories];
}

final class AdvisoryPublishedMessage extends VenueStreamMessage {
  const AdvisoryPublishedMessage(this.advisory);

  final GuestAdvisory advisory;

  @override
  List<Object?> get props => [advisory];
}

final class AdvisoryUpdatedMessage extends VenueStreamMessage {
  const AdvisoryUpdatedMessage(this.advisory);

  final GuestAdvisory advisory;

  @override
  List<Object?> get props => [advisory];
}

final class AdvisoryWithdrawnMessage extends VenueStreamMessage {
  const AdvisoryWithdrawnMessage(this.advisoryId, this.version);

  final String advisoryId;
  final int version;

  @override
  List<Object?> get props => [advisoryId, version];
}

/// Ignored events (heartbeats, unknown names) are dropped by the codec.
final class VenueStreamUnknownMessage extends VenueStreamMessage {
  const VenueStreamUnknownMessage(this.eventName);

  final String eventName;

  @override
  List<Object?> get props => [eventName];
}
