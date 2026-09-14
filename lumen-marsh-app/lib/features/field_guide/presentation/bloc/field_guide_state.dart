import 'package:equatable/equatable.dart';

import '../../domain/field_guide_category.dart';
import '../../domain/field_guide_entry.dart';

sealed class FieldGuideState extends Equatable {
  const FieldGuideState();

  @override
  List<Object?> get props => [];
}

final class FieldGuideInitial extends FieldGuideState {
  const FieldGuideInitial();
}

final class FieldGuideLoading extends FieldGuideState {
  const FieldGuideLoading();
}

final class FieldGuideLoaded extends FieldGuideState {
  const FieldGuideLoaded({
    required this.entries,
    required this.visibleEntries,
    this.query = '',
    this.selectedCategory,
  });

  final List<FieldGuideEntry> entries;
  final List<FieldGuideEntry> visibleEntries;
  final String query;
  final FieldGuideCategory? selectedCategory;

  bool get hasActiveFilters =>
      query.trim().isNotEmpty || selectedCategory != null;

  FieldGuideLoaded copyWith({
    List<FieldGuideEntry>? entries,
    List<FieldGuideEntry>? visibleEntries,
    String? query,
    FieldGuideCategory? selectedCategory,
    bool clearCategory = false,
  }) {
    return FieldGuideLoaded(
      entries: entries ?? this.entries,
      visibleEntries: visibleEntries ?? this.visibleEntries,
      query: query ?? this.query,
      selectedCategory: clearCategory
          ? null
          : (selectedCategory ?? this.selectedCategory),
    );
  }

  @override
  List<Object?> get props => [entries, visibleEntries, query, selectedCategory];
}
