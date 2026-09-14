import 'package:equatable/equatable.dart';

import '../../../../core/errors/app_failure.dart';
import '../../domain/attraction_detail.dart';

sealed class AttractionDetailState extends Equatable {
  const AttractionDetailState();

  @override
  List<Object?> get props => [];
}

final class AttractionDetailInitial extends AttractionDetailState {
  const AttractionDetailInitial();
}

final class AttractionDetailLoading extends AttractionDetailState {
  const AttractionDetailLoading();
}

final class AttractionDetailLoaded extends AttractionDetailState {
  const AttractionDetailLoaded(this.detail);

  final AttractionDetail detail;

  @override
  List<Object?> get props => [detail];
}

final class AttractionDetailFailure extends AttractionDetailState {
  const AttractionDetailFailure(this.failure);

  final AppFailure failure;

  @override
  List<Object?> get props => [failure];
}
