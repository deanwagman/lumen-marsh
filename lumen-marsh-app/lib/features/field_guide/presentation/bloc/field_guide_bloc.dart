import 'package:flutter_bloc/flutter_bloc.dart';

import '../../domain/field_guide_category.dart';
import '../../domain/field_guide_entry.dart';
import '../../domain/field_guide_repository.dart';
import 'field_guide_event.dart';
import 'field_guide_state.dart';

class FieldGuideBloc extends Bloc<FieldGuideEvent, FieldGuideState> {
  FieldGuideBloc({required this.repository})
    : super(const FieldGuideInitial()) {
    on<FieldGuideRequested>(_onRequested);
    on<FieldGuideSearchChanged>(_onSearchChanged);
    on<FieldGuideCategorySelected>(_onCategorySelected);
    on<FieldGuideFiltersCleared>(_onFiltersCleared);
  }

  final FieldGuideRepository repository;

  Future<void> _onRequested(
    FieldGuideRequested event,
    Emitter<FieldGuideState> emit,
  ) async {
    emit(const FieldGuideLoading());
    final entries = await repository.list();
    emit(FieldGuideLoaded(entries: entries, visibleEntries: entries));
  }

  void _onSearchChanged(
    FieldGuideSearchChanged event,
    Emitter<FieldGuideState> emit,
  ) {
    final current = state;
    if (current is! FieldGuideLoaded) {
      return;
    }

    emit(
      current.copyWith(
        query: event.query,
        visibleEntries: _filter(
          entries: current.entries,
          query: event.query,
          category: current.selectedCategory,
        ),
      ),
    );
  }

  void _onCategorySelected(
    FieldGuideCategorySelected event,
    Emitter<FieldGuideState> emit,
  ) {
    final current = state;
    if (current is! FieldGuideLoaded) {
      return;
    }

    emit(
      FieldGuideLoaded(
        entries: current.entries,
        query: current.query,
        selectedCategory: event.category,
        visibleEntries: _filter(
          entries: current.entries,
          query: current.query,
          category: event.category,
        ),
      ),
    );
  }

  void _onFiltersCleared(
    FieldGuideFiltersCleared event,
    Emitter<FieldGuideState> emit,
  ) {
    final current = state;
    if (current is! FieldGuideLoaded) {
      return;
    }

    emit(
      FieldGuideLoaded(
        entries: current.entries,
        visibleEntries: current.entries,
      ),
    );
  }

  static List<FieldGuideEntry> _filter({
    required List<FieldGuideEntry> entries,
    required String query,
    required FieldGuideCategory? category,
  }) {
    final normalized = query.trim().toLowerCase();
    return [
      for (final entry in entries)
        if ((category == null || entry.category == category) &&
            (normalized.isEmpty ||
                entry.title.toLowerCase().contains(normalized) ||
                entry.summary.toLowerCase().contains(normalized) ||
                entry.body.toLowerCase().contains(normalized)))
          entry,
    ];
  }
}
