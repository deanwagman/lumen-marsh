import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/errors/app_failure.dart';
import '../../data/attraction_repository.dart';
import 'attraction_detail_event.dart';
import 'attraction_detail_state.dart';

class AttractionDetailBloc
    extends Bloc<AttractionDetailEvent, AttractionDetailState> {
  AttractionDetailBloc({required this.repository})
    : super(const AttractionDetailInitial()) {
    on<AttractionDetailRequested>(_onRequested);
  }

  final AttractionRepository repository;

  Future<void> _onRequested(
    AttractionDetailRequested event,
    Emitter<AttractionDetailState> emit,
  ) async {
    emit(const AttractionDetailLoading());
    try {
      final detail = await repository.getById(event.id);
      emit(AttractionDetailLoaded(detail));
    } on AppFailure catch (failure) {
      emit(AttractionDetailFailure(failure));
    } catch (_) {
      emit(const AttractionDetailFailure(UnexpectedFailure()));
    }
  }
}
