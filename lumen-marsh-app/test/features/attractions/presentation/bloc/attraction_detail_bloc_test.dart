import 'package:bloc_test/bloc_test.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lumen_marsh_app/core/errors/app_failure.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/bloc/attraction_detail_bloc.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/bloc/attraction_detail_event.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/bloc/attraction_detail_state.dart';
import 'package:mocktail/mocktail.dart';
import 'package:lumen_marsh_app/features/attractions/data/attraction_repository.dart';

import '../../../../helpers/seeded_attractions.dart';

class _MockAttractionRepository extends Mock implements AttractionRepository {}

void main() {
  late _MockAttractionRepository repository;

  setUp(() {
    repository = _MockAttractionRepository();
  });

  blocTest<AttractionDetailBloc, AttractionDetailState>(
    'emits loading followed by loaded detail',
    build: () {
      when(() => repository.getById('mangrove-run'))
          .thenAnswer((_) async => mangroveRunDetail);
      return AttractionDetailBloc(repository: repository);
    },
    act: (bloc) => bloc.add(const AttractionDetailRequested('mangrove-run')),
    expect: () => [
      const AttractionDetailLoading(),
      AttractionDetailLoaded(mangroveRunDetail),
    ],
  );

  blocTest<AttractionDetailBloc, AttractionDetailState>(
    'emits failure when the detail request fails',
    build: () {
      when(() => repository.getById('missing-ride'))
          .thenThrow(const NotFoundFailure());
      return AttractionDetailBloc(repository: repository);
    },
    act: (bloc) => bloc.add(const AttractionDetailRequested('missing-ride')),
    expect: () => [
      const AttractionDetailLoading(),
      const AttractionDetailFailure(NotFoundFailure()),
    ],
  );
}
