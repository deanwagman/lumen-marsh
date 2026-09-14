import 'package:bloc_test/bloc_test.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:lumen_marsh_app/core/errors/app_failure.dart';
import 'package:lumen_marsh_app/core/venue/domain/venue_live.dart';
import 'package:lumen_marsh_app/features/advisories/data/advisory_repository.dart';
import 'package:lumen_marsh_app/features/advisories/domain/advisory_severity.dart';
import 'package:lumen_marsh_app/features/advisories/domain/guest_advisory.dart';
import 'package:lumen_marsh_app/features/advisories/presentation/bloc/advisories_bloc.dart';
import 'package:lumen_marsh_app/features/advisories/presentation/bloc/advisories_event.dart';
import 'package:lumen_marsh_app/features/advisories/presentation/bloc/advisories_state.dart';

class _MockAdvisoryRepository extends Mock implements AdvisoryRepository {}

GuestAdvisory weatherAdvisory({int version = 4, String message = 'Paused.'}) {
  return GuestAdvisory(
    id: 'weather-1',
    severity: AdvisorySeverity.major,
    title: 'Weather advisory',
    message: message,
    affectedAttractionIds: const ['mangrove-run', 'cypress-coil'],
    updatedAt: DateTime.parse('2026-09-01T15:30:00Z'),
    version: version,
  );
}

void main() {
  late _MockAdvisoryRepository repository;

  setUp(() {
    repository = _MockAdvisoryRepository();
    when(() => repository.watchUpdates())
        .thenAnswer((_) => const Stream.empty());
    when(() => repository.watchConnectionStatus())
        .thenAnswer((_) => const Stream<LiveConnectionStatus>.empty());
    when(() => repository.dispose()).thenAnswer((_) async {});
  });

  blocTest<AdvisoriesBloc, AdvisoriesState>(
    'loads active advisories',
    build: () {
      when(() => repository.listActive())
          .thenAnswer((_) async => [weatherAdvisory()]);
      return AdvisoriesBloc(repository: repository);
    },
    act: (bloc) => bloc.add(const AdvisoriesRequested()),
    expect: () => [
      const AdvisoriesLoading(),
      isA<AdvisoriesLoaded>()
          .having((s) => s.advisories, 'advisories', hasLength(1))
          .having((s) => s.advisories.first.title, 'title', 'Weather advisory'),
    ],
  );

  blocTest<AdvisoriesBloc, AdvisoriesState>(
    'successful empty load is AdvisoriesEmpty not failure',
    build: () {
      when(() => repository.listActive()).thenAnswer((_) async => const []);
      return AdvisoriesBloc(repository: repository);
    },
    act: (bloc) => bloc.add(const AdvisoriesRequested()),
    expect: () => [const AdvisoriesLoading(), isA<AdvisoriesEmpty>()],
  );

  blocTest<AdvisoriesBloc, AdvisoriesState>(
    'network failure keeps previous advisories',
    build: () => AdvisoriesBloc(repository: repository),
    seed: () => AdvisoriesLoaded([weatherAdvisory()]),
    setUp: () {
      when(() => repository.listActive()).thenThrow(const NetworkFailure());
    },
    act: (bloc) => bloc.add(const AdvisoriesRefreshed()),
    expect: () => [
      isA<AdvisoriesRefreshing>(),
      isA<AdvisoriesFailure>().having(
        (s) => s.previous,
        'previous',
        hasLength(1),
      ),
    ],
  );

  blocTest<AdvisoriesBloc, AdvisoriesState>(
    'applies newer advisory updates and ignores stale versions',
    build: () {
      when(() => repository.listActive())
          .thenAnswer((_) async => [weatherAdvisory()]);
      return AdvisoriesBloc(repository: repository);
    },
    act: (bloc) async {
      bloc.add(const AdvisoriesRequested());
      await Future<void>.delayed(Duration.zero);
      bloc.add(AdvisoryUpdated(weatherAdvisory(version: 3, message: 'Old.')));
      bloc.add(
        AdvisoryUpdated(
          weatherAdvisory(version: 5, message: 'Updated message.'),
        ),
      );
    },
    expect: () => [
      const AdvisoriesLoading(),
      isA<AdvisoriesLoaded>(),
      isA<AdvisoriesLoaded>().having(
        (s) => s.advisories.single.message,
        'message',
        'Updated message.',
      ),
    ],
  );

  blocTest<AdvisoriesBloc, AdvisoriesState>(
    'withdrawn advisory is removed and tracked against stale republication',
    build: () {
      when(() => repository.listActive())
          .thenAnswer((_) async => [weatherAdvisory()]);
      return AdvisoriesBloc(repository: repository);
    },
    act: (bloc) async {
      bloc.add(const AdvisoriesRequested());
      await Future<void>.delayed(Duration.zero);
      bloc.add(const AdvisoryWithdrawn('weather-1', 6));
      bloc.add(
        AdvisoryUpdated(weatherAdvisory(version: 5, message: 'Too old.')),
      );
    },
    expect: () => [
      const AdvisoriesLoading(),
      isA<AdvisoriesLoaded>(),
      isA<AdvisoriesEmpty>(),
    ],
  );
}
