import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lumen_marsh_app/design_system/components/status/lumen_status_badge.dart';
import 'package:lumen_marsh_app/design_system/components/status/lumen_tone.dart';
import 'package:lumen_marsh_app/design_system/foundations/lumen_spacing.dart';
import 'package:lumen_marsh_app/design_system/theme/lumen_theme.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/widgets/attraction_card.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/widgets/attraction_fact_grid.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/widgets/attraction_hero.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/widgets/wait_time_display.dart';
import 'package:lumen_marsh_app/features/today/presentation/pages/today_page.dart';
import 'package:lumen_marsh_app/app/config/app_config.dart';
import 'package:lumen_marsh_app/features/favorites/presentation/bloc/favorites_bloc.dart';
import 'package:lumen_marsh_app/features/favorites/presentation/bloc/favorites_event.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/bloc/attractions_bloc.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/bloc/attractions_event.dart';
import 'package:lumen_marsh_app/features/attractions/data/attraction_repository.dart';
import 'package:lumen_marsh_app/features/advisories/data/advisory_repository.dart';
import 'package:lumen_marsh_app/features/advisories/presentation/bloc/advisories_bloc.dart';
import 'package:lumen_marsh_app/features/advisories/presentation/bloc/advisories_event.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:mocktail/mocktail.dart';

import '../helpers/seeded_attractions.dart';
import '../helpers/mock_favorites_repository.dart';
import '../helpers/venue_live_test_support.dart';

class _MockAttractionRepository extends Mock implements AttractionRepository {}

class _MockAdvisoryRepository extends Mock implements AdvisoryRepository {}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Widget wrap({
    required Widget child,
    required ThemeData theme,
    Size size = const Size(400, 900),
    TextScaler textScaler = const TextScaler.linear(1),
  }) {
    return MediaQuery(
      data: MediaQueryData(
        size: size,
        disableAnimations: true,
        textScaler: textScaler,
      ),
      child: RepositoryProvider<AppConfig>.value(
        value: const AppConfig(apiBaseUrl: 'http://localhost:8080'),
        child: MaterialApp(
          theme: theme,
          home: Scaffold(body: Center(child: child)),
        ),
      ),
    );
  }

  testWidgets('status badges match light phone golden', (tester) async {
    tester.view.physicalSize = const Size(400, 200);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      wrap(
        theme: LumenTheme.light(),
        child: const Wrap(
          spacing: 8,
          children: [
            LumenStatusBadge(tone: LumenTone.positive, label: 'Operating'),
            LumenStatusBadge(tone: LumenTone.neutral, label: 'Closed'),
            LumenStatusBadge(tone: LumenTone.warning, label: 'Testing'),
            LumenStatusBadge(
              tone: LumenTone.informational,
              label: 'Weather hold',
            ),
            LumenStatusBadge(
              tone: LumenTone.critical,
              label: 'Technical delay',
            ),
          ],
        ),
      ),
    );
    await tester.pumpAndSettle();
    await expectLater(
      find.byType(Wrap),
      matchesGoldenFile('goldens/status_badges_light_phone.png'),
    );
  });

  testWidgets('attraction card matches dark phone golden', (tester) async {
    tester.view.physicalSize = const Size(400, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      wrap(
        theme: LumenTheme.dark(),
        child: AttractionCard(attraction: mangroveRunSummary, onPressed: () {}),
      ),
    );
    await tester.pumpAndSettle();
    await expectLater(
      find.byType(AttractionCard),
      matchesGoldenFile('goldens/attraction_card_dark_phone.png'),
    );
  });

  testWidgets('attraction card matches light tablet golden', (tester) async {
    tester.view.physicalSize = const Size(840, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      wrap(
        theme: LumenTheme.light(),
        size: const Size(840, 900),
        child: AttractionCard(attraction: mangroveRunSummary, onPressed: () {}),
      ),
    );
    await tester.pumpAndSettle();
    await expectLater(
      find.byType(AttractionCard),
      matchesGoldenFile('goldens/attraction_card_light_tablet.png'),
    );
  });

  testWidgets('attraction card matches dark desktop golden', (tester) async {
    tester.view.physicalSize = const Size(1200, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      wrap(
        theme: LumenTheme.dark(),
        size: const Size(1200, 900),
        child: AttractionCard(attraction: mangroveRunSummary, onPressed: () {}),
      ),
    );
    await tester.pumpAndSettle();
    await expectLater(
      find.byType(AttractionCard),
      matchesGoldenFile('goldens/attraction_card_dark_desktop.png'),
    );
  });

  testWidgets('status badges match dark phone golden', (tester) async {
    tester.view.physicalSize = const Size(400, 200);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      wrap(
        theme: LumenTheme.dark(),
        child: const Wrap(
          spacing: 8,
          children: [
            LumenStatusBadge(tone: LumenTone.positive, label: 'Operating'),
            LumenStatusBadge(tone: LumenTone.neutral, label: 'Closed'),
            LumenStatusBadge(tone: LumenTone.warning, label: 'Testing'),
            LumenStatusBadge(
              tone: LumenTone.informational,
              label: 'Weather hold',
            ),
            LumenStatusBadge(
              tone: LumenTone.critical,
              label: 'Technical delay',
            ),
          ],
        ),
      ),
    );
    await tester.pumpAndSettle();
    await expectLater(
      find.byType(Wrap),
      matchesGoldenFile('goldens/status_badges_dark_phone.png'),
    );
  });

  testWidgets('attraction detail matches light phone golden', (tester) async {
    tester.view.physicalSize = const Size(400, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    const detailKey = Key('detail-golden');
    await tester.pumpWidget(
      wrap(
        theme: LumenTheme.light(),
        child: Column(
          key: detailKey,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            AttractionHero(
              detail: mangroveRunDetail,
              apiBaseUrl: 'http://localhost:8080',
              isFavorite: false,
              onFavoriteToggle: () {},
            ),
            const SizedBox(height: LumenSpacing.lg),
            WaitTimeDisplay(
              label: mangroveRunDetail.waitTimeLabel,
              emphasized: true,
            ),
            const SizedBox(height: LumenSpacing.lg),
            AttractionFactGrid(detail: mangroveRunDetail),
          ],
        ),
      ),
    );
    await tester.pumpAndSettle();
    await expectLater(
      find.byKey(detailKey),
      matchesGoldenFile('goldens/attraction_detail_light_phone.png'),
    );
  });

  testWidgets('attraction detail matches dark desktop golden', (tester) async {
    tester.view.physicalSize = const Size(1200, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    const detailKey = Key('detail-desktop-golden');
    await tester.pumpWidget(
      wrap(
        theme: LumenTheme.dark(),
        size: const Size(1200, 900),
        child: Column(
          key: detailKey,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            AttractionHero(
              detail: mangroveRunDetail,
              apiBaseUrl: 'http://localhost:8080',
              isFavorite: false,
              onFavoriteToggle: () {},
            ),
            const SizedBox(height: LumenSpacing.lg),
            WaitTimeDisplay(
              label: mangroveRunDetail.waitTimeLabel,
              emphasized: true,
            ),
            const SizedBox(height: LumenSpacing.lg),
            AttractionFactGrid(detail: mangroveRunDetail),
          ],
        ),
      ),
    );
    await tester.pumpAndSettle();
    await expectLater(
      find.byKey(detailKey),
      matchesGoldenFile('goldens/attraction_detail_dark_desktop.png'),
    );
  });

  testWidgets('attraction detail supports large text on phone', (tester) async {
    tester.view.physicalSize = const Size(400, 1200);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    const detailKey = Key('detail-large-text');
    await tester.pumpWidget(
      wrap(
        theme: LumenTheme.light(),
        textScaler: TextScaler.linear(2),
        child: SingleChildScrollView(
          child: Column(
            key: detailKey,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              AttractionHero(
                detail: mangroveRunDetail,
                apiBaseUrl: 'http://localhost:8080',
                isFavorite: false,
                onFavoriteToggle: () {},
              ),
              const SizedBox(height: LumenSpacing.lg),
              WaitTimeDisplay(
                label: mangroveRunDetail.waitTimeLabel,
                emphasized: true,
              ),
              const SizedBox(height: LumenSpacing.lg),
              AttractionFactGrid(detail: mangroveRunDetail),
            ],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await expectLater(
      find.byKey(detailKey),
      matchesGoldenFile('goldens/attraction_detail_large_text_phone.png'),
    );
  });

  testWidgets('today dashboard matches light phone golden', (tester) async {
    tester.view.physicalSize = const Size(400, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final repository = _MockAttractionRepository();
    final advisoryRepository = _MockAdvisoryRepository();
    when(() => repository.list()).thenAnswer((_) async => seededAttractions);
    when(() => advisoryRepository.listActive())
        .thenAnswer((_) async => const []);
    stubIdleAttractionLiveStream(repository);
    stubIdleAdvisoryLiveStream(advisoryRepository);

    await tester.pumpWidget(
      wrap(
        theme: LumenTheme.light(),
        child: MultiBlocProvider(
          providers: [
            BlocProvider(
              create: (_) =>
                  AttractionsBloc(repository: repository)
                    ..add(const AttractionsRequested()),
            ),
            BlocProvider(
              create: (_) =>
                  AdvisoriesBloc(repository: advisoryRepository)
                    ..add(const AdvisoriesRequested()),
            ),
            BlocProvider(
              create: (_) {
                final favoritesRepository = MockFavoritesRepository();
                stubEmptyFavorites(favoritesRepository);
                return FavoritesBloc(repository: favoritesRepository)
                  ..add(const FavoritesHydrated());
              },
            ),
          ],
          child: const TodayPage(),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await expectLater(
      find.byKey(const Key('today-dashboard')),
      matchesGoldenFile('goldens/today_dashboard_light_phone.png'),
    );
  });

  testWidgets('today dashboard matches dark desktop golden', (tester) async {
    tester.view.physicalSize = const Size(1200, 900);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final repository = _MockAttractionRepository();
    final advisoryRepository = _MockAdvisoryRepository();
    when(() => repository.list()).thenAnswer((_) async => seededAttractions);
    when(() => advisoryRepository.listActive())
        .thenAnswer((_) async => const []);
    stubIdleAttractionLiveStream(repository);
    stubIdleAdvisoryLiveStream(advisoryRepository);

    await tester.pumpWidget(
      wrap(
        theme: LumenTheme.dark(),
        size: const Size(1200, 900),
        child: MultiBlocProvider(
          providers: [
            BlocProvider(
              create: (_) =>
                  AttractionsBloc(repository: repository)
                    ..add(const AttractionsRequested()),
            ),
            BlocProvider(
              create: (_) =>
                  AdvisoriesBloc(repository: advisoryRepository)
                    ..add(const AdvisoriesRequested()),
            ),
            BlocProvider(
              create: (_) {
                final favoritesRepository = MockFavoritesRepository();
                stubEmptyFavorites(favoritesRepository);
                return FavoritesBloc(repository: favoritesRepository)
                  ..add(const FavoritesHydrated());
              },
            ),
          ],
          child: const TodayPage(),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await expectLater(
      find.byKey(const Key('today-dashboard')),
      matchesGoldenFile('goldens/today_dashboard_dark_desktop.png'),
    );
  });
}
