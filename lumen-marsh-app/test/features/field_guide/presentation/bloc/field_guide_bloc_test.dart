import 'package:bloc_test/bloc_test.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lumen_marsh_app/features/field_guide/domain/field_guide_category.dart';
import 'package:lumen_marsh_app/features/field_guide/presentation/bloc/field_guide_bloc.dart';
import 'package:lumen_marsh_app/features/field_guide/presentation/bloc/field_guide_event.dart';
import 'package:lumen_marsh_app/features/field_guide/presentation/bloc/field_guide_state.dart';

import '../../../../helpers/mock_field_guide_repository.dart';
import '../../../../helpers/seeded_field_guide.dart';

void main() {
  late MockFieldGuideRepository repository;

  setUp(() {
    repository = MockFieldGuideRepository();
    stubFieldGuideCatalog(repository);
  });

  blocTest<FieldGuideBloc, FieldGuideState>(
    'loads the catalog',
    build: () => FieldGuideBloc(repository: repository),
    act: (bloc) => bloc.add(const FieldGuideRequested()),
    expect: () => [
      const FieldGuideLoading(),
      FieldGuideLoaded(
        entries: seededFieldGuideEntries,
        visibleEntries: seededFieldGuideEntries,
      ),
    ],
  );

  blocTest<FieldGuideBloc, FieldGuideState>(
    'filters by search query across title summary and body',
    build: () => FieldGuideBloc(repository: repository),
    seed: () => FieldGuideLoaded(
      entries: seededFieldGuideEntries,
      visibleEntries: seededFieldGuideEntries,
    ),
    act: (bloc) => bloc.add(const FieldGuideSearchChanged('stormglass')),
    expect: () => [
      FieldGuideLoaded(
        entries: seededFieldGuideEntries,
        visibleEntries: const [stormglassNotes],
        query: 'stormglass',
      ),
    ],
  );

  blocTest<FieldGuideBloc, FieldGuideState>(
    'filters by category',
    build: () => FieldGuideBloc(repository: repository),
    seed: () => FieldGuideLoaded(
      entries: seededFieldGuideEntries,
      visibleEntries: seededFieldGuideEntries,
    ),
    act: (bloc) =>
        bloc.add(const FieldGuideCategorySelected(FieldGuideCategory.wildlife)),
    expect: () => [
      FieldGuideLoaded(
        entries: seededFieldGuideEntries,
        visibleEntries: const [marshHeron],
        selectedCategory: FieldGuideCategory.wildlife,
      ),
    ],
  );

  blocTest<FieldGuideBloc, FieldGuideState>(
    'combines search and category filters',
    build: () => FieldGuideBloc(repository: repository),
    seed: () => FieldGuideLoaded(
      entries: seededFieldGuideEntries,
      visibleEntries: seededFieldGuideEntries,
      selectedCategory: FieldGuideCategory.flora,
    ),
    act: (bloc) => bloc.add(const FieldGuideSearchChanged('orchid')),
    expect: () => [
      FieldGuideLoaded(
        entries: seededFieldGuideEntries,
        visibleEntries: const [ghostOrchid],
        query: 'orchid',
        selectedCategory: FieldGuideCategory.flora,
      ),
    ],
  );

  blocTest<FieldGuideBloc, FieldGuideState>(
    'clears filters',
    build: () => FieldGuideBloc(repository: repository),
    seed: () => FieldGuideLoaded(
      entries: seededFieldGuideEntries,
      visibleEntries: const [ghostOrchid],
      query: 'orchid',
      selectedCategory: FieldGuideCategory.flora,
    ),
    act: (bloc) => bloc.add(const FieldGuideFiltersCleared()),
    expect: () => [
      FieldGuideLoaded(
        entries: seededFieldGuideEntries,
        visibleEntries: seededFieldGuideEntries,
      ),
    ],
  );
}
