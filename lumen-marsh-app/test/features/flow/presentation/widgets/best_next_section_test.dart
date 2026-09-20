import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:lumen_marsh_app/app/config/app_config.dart';
import 'package:lumen_marsh_app/features/flow/data/flow_repository.dart';
import 'package:lumen_marsh_app/features/flow/domain/guest_wait.dart';
import 'package:lumen_marsh_app/features/flow/domain/park_zone.dart';
import 'package:lumen_marsh_app/features/flow/presentation/bloc/flow_bloc.dart';
import 'package:lumen_marsh_app/features/flow/presentation/bloc/flow_event.dart';
import 'package:lumen_marsh_app/features/flow/presentation/widgets/best_next_section.dart';
import 'package:lumen_marsh_app/features/flow/presentation/widgets/wait_outlook.dart';
import 'package:lumen_marsh_app/design_system/theme/lumen_theme.dart';

import '../../../../helpers/seeded_attractions.dart';
import '../../../../helpers/seeded_flow.dart';
import '../../../../helpers/venue_live_test_support.dart';

class _MockFlowRepository extends Mock implements FlowRepository {}

void main() {
  late _MockFlowRepository repository;

  setUpAll(() {
    registerFallbackValue(const FlowRequested());
    registerFallbackValue(const FlowRefreshed());
    registerFallbackValue(
      const FlowOriginZoneSelected(ParkZone.luminousWetlandsId),
    );
  });

  setUp(() {
    repository = _MockFlowRepository();
    stubIdleFlowLiveStream(repository);
    when(() => repository.overview()).thenAnswer(
      (_) async => GuestFlowOverview(
        attractions: [mangroveWait, cypressWait, stormglassWait],
        publishedGuidance: const [],
        updatedAt: DateTime.parse('2026-09-15T18:30:00Z'),
        simulated: true,
      ),
    );
  });

  Widget wrap({required Widget child, Size size = const Size(400, 900)}) {
    return MediaQuery(
      data: MediaQueryData(size: size, disableAnimations: true),
      child: RepositoryProvider<AppConfig>.value(
        value: const AppConfig(apiBaseUrl: 'http://localhost:8080'),
        child: BlocProvider(
          create: (_) =>
              FlowBloc(repository: repository)..add(const FlowRequested()),
          child: MaterialApp(
            theme: LumenTheme.light(),
            home: Scaffold(body: SingleChildScrollView(child: child)),
          ),
        ),
      ),
    );
  }

  testWidgets('ranks operating attractions as Best Next Experiences', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(400, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      wrap(
        child: BestNextSection(
          catalog: seededAttractions,
          favoriteIds: const {},
          onAttractionPressed: (_) {},
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Best Next Experiences'), findsOneWidget);
    expect(find.text('Simulated demonstration data'), findsWidgets);
    expect(
      find.textContaining('Cypress Coil is a good next choice'),
      findsOneWidget,
    );
    expect(find.byKey(const Key('best-next-cypress-coil')), findsOneWidget);
  });

  testWidgets('removes a closed attraction and shows published guidance', (
    tester,
  ) async {
    when(() => repository.overview()).thenAnswer(
      (_) async => GuestFlowOverview(
        attractions: [closedMangroveWait, cypressWait],
        publishedGuidance: [publishedGuidance],
        updatedAt: DateTime.parse('2026-09-15T18:31:00Z'),
        simulated: true,
      ),
    );

    await tester.pumpWidget(
      wrap(
        child: BestNextSection(
          catalog: seededAttractions,
          favoriteIds: const {'mangrove-run'},
          onAttractionPressed: (_) {},
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('best-next-mangrove-run')), findsNothing);
    expect(find.byKey(const Key('best-next-cypress-coil')), findsOneWidget);
    expect(find.text(publishedGuidance.guestMessage!), findsWidgets);
  });

  testWidgets('warns when wait data is stale', (tester) async {
    when(() => repository.overview()).thenAnswer(
      (_) async => GuestFlowOverview(
        attractions: [sampleGuestWait(freshness: QueueFreshness.stale)],
        publishedGuidance: const [],
        updatedAt: DateTime.parse('2026-09-15T18:30:00Z'),
        simulated: true,
      ),
    );

    await tester.pumpWidget(
      wrap(
        child: BestNextSection(
          catalog: seededAttractions,
          favoriteIds: const {},
          onAttractionPressed: (_) {},
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Stale wait data'), findsWidgets);
    expect(
      find.textContaining('Rankings stay frozen until fresh telemetry arrives'),
      findsOneWidget,
    );
  });

  testWidgets('wait outlook uses rounded language and a relative timestamp', (
    tester,
  ) async {
    final wait = sampleGuestWait(
      updatedAt: DateTime.now()
          .toUtc()
          .subtract(const Duration(minutes: 1))
          .toIso8601String(),
    );

    await tester.pumpWidget(wrap(child: WaitOutlook(wait: wait)));

    expect(find.text('Wait outlook'), findsOneWidget);
    expect(find.text('Now: About 20 minutes'), findsOneWidget);
    expect(find.text('In 30 minutes: About 25 minutes'), findsOneWidget);
    expect(find.text('Trend: Likely stable'), findsOneWidget);
    expect(find.textContaining('Updated:'), findsOneWidget);
    expect(find.text('Simulated demonstration data'), findsOneWidget);
  });

  testWidgets('wait outlook hides the 30-minute forecast when stale', (
    tester,
  ) async {
    await tester.pumpWidget(
      wrap(
        child: WaitOutlook(
          wait: sampleGuestWait(freshness: QueueFreshness.stale),
        ),
      ),
    );

    expect(find.text('Stale wait data'), findsOneWidget);
    expect(
      find.text('In 30 minutes: Not available while data is stale'),
      findsOneWidget,
    );
  });

  testWidgets('supports keyboard focus on Best Next cards on a wide layout', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(900, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    var opened = '';
    await tester.pumpWidget(
      wrap(
        size: const Size(900, 900),
        child: BestNextSection(
          catalog: seededAttractions,
          favoriteIds: const {},
          onAttractionPressed: (id) => opened = id,
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('best-next-cypress-coil')));
    await tester.pump();

    expect(opened, 'cypress-coil');
  });
}
