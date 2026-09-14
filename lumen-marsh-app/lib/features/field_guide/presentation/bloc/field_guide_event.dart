import 'package:equatable/equatable.dart';

import '../../domain/field_guide_category.dart';

sealed class FieldGuideEvent extends Equatable {
  const FieldGuideEvent();

  @override
  List<Object?> get props => [];
}

final class FieldGuideRequested extends FieldGuideEvent {
  const FieldGuideRequested();
}

final class FieldGuideSearchChanged extends FieldGuideEvent {
  const FieldGuideSearchChanged(this.query);

  final String query;

  @override
  List<Object?> get props => [query];
}

final class FieldGuideCategorySelected extends FieldGuideEvent {
  const FieldGuideCategorySelected(this.category);

  final FieldGuideCategory? category;

  @override
  List<Object?> get props => [category];
}

final class FieldGuideFiltersCleared extends FieldGuideEvent {
  const FieldGuideFiltersCleared();
}
