import 'package:equatable/equatable.dart';

sealed class AttractionDetailEvent extends Equatable {
  const AttractionDetailEvent();

  @override
  List<Object?> get props => [];
}

final class AttractionDetailRequested extends AttractionDetailEvent {
  const AttractionDetailRequested(this.id);

  final String id;

  @override
  List<Object?> get props => [id];
}
