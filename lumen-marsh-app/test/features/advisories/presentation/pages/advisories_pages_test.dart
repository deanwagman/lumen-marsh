import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:mocktail/mocktail.dart';
import 'package:lumen_marsh_app/design_system/theme/lumen_theme.dart';
import 'package:lumen_marsh_app/features/advisories/domain/advisory_severity.dart';
import 'package:lumen_marsh_app/features/advisories/domain/guest_advisory.dart';
import 'package:lumen_marsh_app/features/advisories/presentation/bloc/advisories_bloc.dart';
import 'package:lumen_marsh_app/features/advisories/presentation/bloc/advisories_state.dart';
import 'package:lumen_marsh_app/features/advisories/presentation/pages/advisories_page.dart';
import 'package:lumen_marsh_app/features/advisories/presentation/pages/advisory_detail_page.dart';
import 'package:lumen_marsh_app/features/advisories/presentation/widgets/guest_advisory_banner.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/bloc/attractions_bloc.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/bloc/attractions_state.dart';

class _MockAdvisoriesBloc extends Mock implements AdvisoriesBloc {}

class _MockAttractionsBloc extends Mock implements AttractionsBloc {}

GuestAdvisory sampleAdvisory() {
  return GuestAdvisory(
    id: 'weather-1',
    severity: AdvisorySeverity.major,
    title: 'Weather advisory',
    message: 'Some outdoor attractions are temporarily paused.',
    affectedAttractionIds: const ['mangrove-run'],
    updatedAt: DateTime.parse('2026-09-01T15:30:00Z'),
    version: 1,
  );
}

void main() {
  late _MockAdvisoriesBloc advisoriesBloc;
  late _MockAttractionsBloc attractionsBloc;

  setUp(() {
    advisoriesBloc = _MockAdvisoriesBloc();
    attractionsBloc = _MockAttractionsBloc();
    when(() => advisoriesBloc.stream).thenAnswer((_) => const Stream.empty());
    when(() => attractionsBloc.stream).thenAnswer((_) => const Stream.empty());
    when(() => attractionsBloc.state).thenReturn(const AttractionsLoading());
    when(() => advisoriesBloc.close()).thenAnswer((_) async {});
    when(() => attractionsBloc.close()).thenAnswer((_) async {});
  });

  Widget buildApp({required String initialLocation}) {
    return MediaQuery(
      data: const MediaQueryData(size: Size(400, 1200)),
      child: MultiBlocProvider(
        providers: [
          BlocProvider<AdvisoriesBloc>.value(value: advisoriesBloc),
          BlocProvider<AttractionsBloc>.value(value: attractionsBloc),
        ],
        child: MaterialApp.router(
          theme: LumenTheme.light(),
          routerConfig: GoRouter(
            initialLocation: initialLocation,
            routes: [
              GoRoute(
                path: '/advisories',
                builder: (context, state) => const AdvisoriesPage(),
                routes: [
                  GoRoute(
                    path: ':id',
                    builder: (context, state) => AdvisoryDetailPage(
                      advisoryId: state.pathParameters['id']!,
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

  testWidgets('inbox opens advisory detail when banner is tapped', (
    tester,
  ) async {
    when(() => advisoriesBloc.state)
        .thenReturn(AdvisoriesLoaded([sampleAdvisory()]));

    await tester.pumpWidget(buildApp(initialLocation: '/advisories'));
    await tester.pumpAndSettle();

    expect(find.text('Weather advisory'), findsOneWidget);
    await tester.tap(find.byKey(const Key('advisory-weather-1')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('advisory-detail-weather-1')), findsOneWidget);
    expect(
      find.text('Some outdoor attractions are temporarily paused.'),
      findsWidgets,
    );
  });

  testWidgets('missing advisory shows cleared state', (tester) async {
    when(() => advisoriesBloc.state).thenReturn(const AdvisoriesEmpty());

    await tester.pumpWidget(buildApp(initialLocation: '/advisories/missing'));
    await tester.pumpAndSettle();

    expect(find.text('Advisory cleared'), findsOneWidget);
    expect(find.text('Back to advisories'), findsOneWidget);
  });

  testWidgets('empty inbox shows no active park advisories', (tester) async {
    when(() => advisoriesBloc.state).thenReturn(const AdvisoriesEmpty());

    await tester.pumpWidget(buildApp(initialLocation: '/advisories'));
    await tester.pumpAndSettle();

    expect(find.text('No active park advisories'), findsOneWidget);
  });

  testWidgets('AdvisoryContent exposes onPressed for shared taps', (
    tester,
  ) async {
    var tapped = false;
    await tester.pumpWidget(
      MaterialApp(
        theme: LumenTheme.light(),
        home: Scaffold(
          body: AdvisoryContent(
            advisory: sampleAdvisory(),
            onPressed: () => tapped = true,
          ),
        ),
      ),
    );

    await tester.tap(find.byType(AdvisoryContent));
    expect(tapped, isTrue);
  });
}
