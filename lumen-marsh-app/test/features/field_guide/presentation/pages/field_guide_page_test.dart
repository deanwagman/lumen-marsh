import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:mocktail/mocktail.dart';
import 'package:lumen_marsh_app/design_system/theme/lumen_theme.dart';
import 'package:lumen_marsh_app/features/field_guide/domain/field_guide_category.dart';
import 'package:lumen_marsh_app/features/field_guide/domain/field_guide_repository.dart';
import 'package:lumen_marsh_app/features/field_guide/presentation/bloc/field_guide_bloc.dart';
import 'package:lumen_marsh_app/features/field_guide/presentation/bloc/field_guide_event.dart';
import 'package:lumen_marsh_app/features/field_guide/presentation/bloc/field_guide_state.dart';
import 'package:lumen_marsh_app/features/field_guide/presentation/pages/field_guide_page.dart';
import 'package:lumen_marsh_app/features/field_guide/presentation/widgets/field_guide_entry_card.dart';

import '../../../../helpers/mock_field_guide_repository.dart';
import '../../../../helpers/seeded_field_guide.dart';

class _MockFieldGuideBloc extends Mock implements FieldGuideBloc {}

void main() {
  late MockFieldGuideRepository repository;
  late _MockFieldGuideBloc bloc;

  setUpAll(() {
    registerFallbackValue(const FieldGuideRequested());
    registerFallbackValue(const FieldGuideSearchChanged(''));
    registerFallbackValue(const FieldGuideCategorySelected(null));
    registerFallbackValue(const FieldGuideFiltersCleared());
  });

  setUp(() {
    repository = MockFieldGuideRepository();
    bloc = _MockFieldGuideBloc();
    stubFieldGuideCatalog(repository);
  });

  void stubState(FieldGuideState state) {
    when(() => bloc.state).thenReturn(state);
    when(() => bloc.stream).thenAnswer((_) => Stream.value(state));
  }

  Widget buildApp({Size size = const Size(400, 900)}) {
    return MediaQuery(
      data: MediaQueryData(size: size, disableAnimations: true),
      child: RepositoryProvider<FieldGuideRepository>.value(
        value: repository,
        child: BlocProvider<FieldGuideBloc>.value(
          value: bloc,
          child: MaterialApp.router(
            theme: LumenTheme.light(),
            routerConfig: GoRouter(
              initialLocation: '/guide',
              routes: [
                GoRoute(
                  path: '/guide',
                  builder: (context, state) => const FieldGuidePage(),
                  routes: [
                    GoRoute(
                      path: ':id',
                      builder: (context, state) => Scaffold(
                        body: Text('Detail ${state.pathParameters['id']}'),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  testWidgets('renders catalog entries on phone', (tester) async {
    tester.view.physicalSize = const Size(400, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    stubState(
      FieldGuideLoaded(
        entries: seededFieldGuideEntries,
        visibleEntries: seededFieldGuideEntries,
      ),
    );

    await tester.pumpWidget(buildApp());
    await tester.pumpAndSettle();

    expect(find.text('Field journal'), findsOneWidget);
    expect(find.text('Lumen Ghost Orchid'), findsOneWidget);
    await tester.scrollUntilVisible(
      find.text('Cypress Basin Dusk Heron'),
      300,
      scrollable: find.byType(Scrollable).first,
    );
    expect(find.text('Cypress Basin Dusk Heron'), findsOneWidget);
    expect(find.byKey(const Key('field-guide-catalog')), findsOneWidget);
  });

  testWidgets('uses a multi-column grid on wide layouts', (tester) async {
    tester.view.physicalSize = const Size(1200, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    stubState(
      FieldGuideLoaded(
        entries: seededFieldGuideEntries,
        visibleEntries: seededFieldGuideEntries,
      ),
    );

    await tester.pumpWidget(buildApp(size: const Size(1200, 900)));
    await tester.pumpAndSettle();

    final grid = tester.widget<SliverGrid>(find.byType(SliverGrid));
    final delegate =
        grid.gridDelegate as SliverGridDelegateWithFixedCrossAxisCount;
    expect(delegate.crossAxisCount, 3);
  });

  testWidgets('category chip narrows the catalog', (tester) async {
    stubState(
      FieldGuideLoaded(
        entries: seededFieldGuideEntries,
        visibleEntries: seededFieldGuideEntries,
      ),
    );

    await tester.pumpWidget(buildApp());
    await tester.pumpAndSettle();

    await tester.tap(find.widgetWithText(ChoiceChip, 'Wildlife'));
    await tester.pump();

    verify(
      () => bloc.add(
        const FieldGuideCategorySelected(FieldGuideCategory.wildlife),
      ),
    ).called(1);
  });

  testWidgets('empty search shows clearable empty state', (tester) async {
    tester.view.physicalSize = const Size(400, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    stubState(
      FieldGuideLoaded(
        entries: seededFieldGuideEntries,
        visibleEntries: const [],
        query: 'zzzz',
      ),
    );

    await tester.pumpWidget(buildApp());
    await tester.pumpAndSettle();

    expect(find.text('No entries match'), findsOneWidget);
    await tester.scrollUntilVisible(
      find.text('Clear filters'),
      200,
      scrollable: find.byType(Scrollable).first,
    );
    await tester.tap(find.text('Clear filters'));
    await tester.pump();

    verify(() => bloc.add(const FieldGuideFiltersCleared())).called(1);
  });

  testWidgets('empty catalog shows quiet journal state', (tester) async {
    stubState(const FieldGuideLoaded(entries: [], visibleEntries: []));

    await tester.pumpWidget(buildApp());
    await tester.pumpAndSettle();

    expect(find.text('Field Guide is quiet'), findsOneWidget);
  });

  testWidgets('tap card opens detail route', (tester) async {
    var openedId = '';
    stubState(
      FieldGuideLoaded(
        entries: seededFieldGuideEntries,
        visibleEntries: const [ghostOrchid],
      ),
    );

    await tester.pumpWidget(
      MediaQuery(
        data: const MediaQueryData(
          size: Size(400, 900),
          disableAnimations: true,
        ),
        child: RepositoryProvider<FieldGuideRepository>.value(
          value: repository,
          child: BlocProvider<FieldGuideBloc>.value(
            value: bloc,
            child: MaterialApp(
              theme: LumenTheme.light(),
              home: Scaffold(
                body: FieldGuideEntryCard(
                  entry: ghostOrchid,
                  onPressed: () => openedId = ghostOrchid.id,
                ),
              ),
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('field-guide-card-ghost-orchid')));
    await tester.pump();

    expect(openedId, 'ghost-orchid');
  });
}
