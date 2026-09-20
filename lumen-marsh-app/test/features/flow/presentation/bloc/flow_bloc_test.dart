import 'package:bloc_test/bloc_test.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:lumen_marsh_app/core/errors/app_failure.dart';
import 'package:lumen_marsh_app/core/venue/domain/venue_stream_message.dart';
import 'package:lumen_marsh_app/features/flow/data/flow_repository.dart';
import 'package:lumen_marsh_app/features/flow/domain/guest_wait.dart';
import 'package:lumen_marsh_app/features/flow/presentation/bloc/flow_bloc.dart';
import 'package:lumen_marsh_app/features/flow/presentation/bloc/flow_event.dart';
import 'package:lumen_marsh_app/features/flow/presentation/bloc/flow_state.dart';

import '../../../../helpers/seeded_flow.dart';
import '../../../../helpers/venue_live_test_support.dart';

class _MockFlowRepository extends Mock implements FlowRepository {}

void main() {
  late _MockFlowRepository repository;

  setUp(() {
    repository = _MockFlowRepository();
    stubIdleFlowLiveStream(repository);
  });

  blocTest<FlowBloc, FlowState>(
    'loads a guest flow overview',
    build: () {
      when(() => repository.overview()).thenAnswer(
        (_) async => GuestFlowOverview(
          attractions: [mangroveWait, cypressWait],
          publishedGuidance: const [],
          updatedAt: DateTime.parse('2026-09-15T18:30:00Z'),
          simulated: true,
        ),
      );
      return FlowBloc(repository: repository);
    },
    act: (bloc) => bloc.add(const FlowRequested()),
    expect: () => [
      const FlowLoading(),
      isA<FlowLoaded>()
          .having((s) => s.waits, 'waits', hasLength(2))
          .having((s) => s.simulated, 'simulated', isTrue),
    ],
  );

  blocTest<FlowBloc, FlowState>(
    'empty overview is FlowEmpty not failure',
    build: () {
      when(() => repository.overview())
          .thenAnswer((_) async => emptyGuestFlowOverview);
      return FlowBloc(repository: repository);
    },
    act: (bloc) => bloc.add(const FlowRequested()),
    expect: () => [const FlowLoading(), isA<FlowEmpty>()],
  );

  blocTest<FlowBloc, FlowState>(
    'applies live wait updates and ignores older timestamps',
    build: () => FlowBloc(repository: repository),
    seed: () => FlowLoaded(
      waits: [mangroveWait],
      guidance: const [],
      updatedAt: mangroveWait.updatedAt,
      simulated: true,
    ),
    act: (bloc) {
      bloc.add(
        FlowWaitUpdated(
          sampleGuestWait(
            postedWaitMinutes: 10,
            waitOutlook: 'About 10 minutes',
            updatedAt: '2026-09-15T18:29:00Z',
          ),
        ),
      );
      bloc.add(
        FlowWaitUpdated(
          sampleGuestWait(
            postedWaitMinutes: 30,
            waitOutlook: 'About 30 minutes',
            forecast30Minutes: null,
            updatedAt: '2026-09-15T18:32:00Z',
          ),
        ),
      );
    },
    expect: () => [
      isA<FlowLoaded>()
          .having((s) => s.waits.single.postedWaitMinutes, 'posted', 30)
          .having(
            (s) => s.waits.single.forecast30Minutes,
            'kept forecast',
            'About 25 minutes',
          ),
    ],
  );

  blocTest<FlowBloc, FlowState>(
    'publishes and withdraws guest guidance live',
    build: () => FlowBloc(repository: repository),
    seed: () => FlowLoaded(
      waits: [cypressWait],
      guidance: const [],
      updatedAt: cypressWait.updatedAt,
      simulated: true,
    ),
    act: (bloc) {
      bloc.add(FlowGuidancePublished(publishedGuidance));
      bloc.add(FlowGuidanceWithdrawn(publishedGuidance.recommendationId));
    },
    expect: () => [
      isA<FlowLoaded>().having((s) => s.guidance, 'guidance', hasLength(1)),
      isA<FlowLoaded>().having((s) => s.guidance, 'guidance', isEmpty),
    ],
  );

  blocTest<FlowBloc, FlowState>(
    'network failure keeps previous waits',
    build: () => FlowBloc(repository: repository),
    seed: () => FlowLoaded(
      waits: [mangroveWait],
      guidance: const [],
      updatedAt: mangroveWait.updatedAt,
      simulated: true,
    ),
    setUp: () {
      when(() => repository.overview()).thenThrow(const NetworkFailure());
    },
    act: (bloc) => bloc.add(const FlowRefreshed()),
    expect: () => [
      isA<FlowFailure>().having(
        (s) => s.previous?.waits,
        'previous',
        hasLength(1),
      ),
    ],
  );

  blocTest<FlowBloc, FlowState>(
    'applies a snapshot from the live stream',
    build: () {
      when(() => repository.overview())
          .thenAnswer((_) async => emptyGuestFlowOverview);
      when(() => repository.watchUpdates()).thenAnswer(
        (_) => Stream.value(
          GuestFlowOverviewMessage(
            GuestFlowOverview(
              attractions: [cypressWait],
              publishedGuidance: [publishedGuidance],
              updatedAt: cypressWait.updatedAt,
              simulated: true,
            ),
          ),
        ),
      );
      return FlowBloc(repository: repository);
    },
    act: (bloc) => bloc.add(const FlowRequested()),
    wait: const Duration(milliseconds: 10),
    expect: () => [
      const FlowLoading(),
      isA<FlowEmpty>(),
      isA<FlowLoaded>()
          .having((s) => s.waits.single.attractionId, 'id', 'cypress-coil')
          .having((s) => s.guidance, 'guidance', hasLength(1)),
    ],
  );
}
