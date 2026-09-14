import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:lumen_marsh_app/design_system/theme/lumen_theme.dart';
import 'package:lumen_marsh_app/features/field_guide/domain/field_guide_repository.dart';
import 'package:lumen_marsh_app/features/field_guide/presentation/pages/field_guide_detail_page.dart';

import '../../../../helpers/mock_field_guide_repository.dart';

void main() {
  late MockFieldGuideRepository repository;

  setUp(() {
    repository = MockFieldGuideRepository();
    stubFieldGuideCatalog(repository);
  });

  Widget buildApp({
    required String entryId,
    TextScaler textScaler = const TextScaler.linear(1),
  }) {
    return MediaQuery(
      data: MediaQueryData(size: const Size(400, 1200), textScaler: textScaler),
      child: RepositoryProvider<FieldGuideRepository>.value(
        value: repository,
        child: MaterialApp.router(
          theme: LumenTheme.light(),
          routerConfig: GoRouter(
            initialLocation: '/guide/$entryId',
            routes: [
              GoRoute(
                path: '/guide',
                builder: (context, state) =>
                    const Scaffold(body: Text('Catalog')),
                routes: [
                  GoRoute(
                    path: ':id',
                    builder: (context, state) => FieldGuideDetailPage(
                      entryId: state.pathParameters['id']!,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  testWidgets('renders a known entry', (tester) async {
    await tester.pumpWidget(buildApp(entryId: 'ghost-orchid'));
    await tester.pumpAndSettle();

    expect(find.text('Lumen Ghost Orchid'), findsOneWidget);
    expect(find.text('Flora'), findsOneWidget);
    expect(find.textContaining('Field observers first logged'), findsOneWidget);
  });

  testWidgets('unknown entry shows not found state', (tester) async {
    await tester.pumpWidget(buildApp(entryId: 'missing-entry'));
    await tester.pumpAndSettle();

    expect(find.text('Entry not found'), findsOneWidget);
  });

  testWidgets('remains scrollable at 200% text scale', (tester) async {
    tester.view.physicalSize = const Size(400, 800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      buildApp(entryId: 'ghost-orchid', textScaler: const TextScaler.linear(2)),
    );
    await tester.pumpAndSettle();

    expect(find.byType(ListView), findsOneWidget);
    expect(find.text('Lumen Ghost Orchid'), findsOneWidget);
    await tester.drag(find.byType(ListView), const Offset(0, -200));
    await tester.pumpAndSettle();
  });
}
