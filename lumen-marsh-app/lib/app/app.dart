import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../core/venue/data/venue_event_hub.dart';
import '../core/venue/presentation/venue_lifecycle_binder.dart';
import '../features/advisories/data/advisory_repository.dart';
import '../features/advisories/presentation/bloc/advisories_bloc.dart';
import '../features/advisories/presentation/bloc/advisories_event.dart';
import '../features/attractions/data/attraction_repository.dart';
import '../features/attractions/presentation/bloc/attractions_bloc.dart';
import '../features/attractions/presentation/bloc/attractions_event.dart';
import '../features/favorites/domain/favorites_repository.dart';
import '../features/favorites/presentation/bloc/favorites_bloc.dart';
import '../features/favorites/presentation/bloc/favorites_event.dart';
import '../features/field_guide/domain/field_guide_repository.dart';
import '../features/field_guide/presentation/bloc/field_guide_bloc.dart';
import '../features/field_guide/presentation/bloc/field_guide_event.dart';
import '../features/flow/data/flow_repository.dart';
import '../features/flow/presentation/bloc/flow_bloc.dart';
import '../features/flow/presentation/bloc/flow_event.dart';
import 'config/app_config.dart';
import 'routing/app_router.dart';
import '../design_system/theme/lumen_theme.dart';

class LumenMarshApp extends StatelessWidget {
  LumenMarshApp({
    super.key,
    required this.config,
    required this.eventHub,
    required this.attractionRepository,
    required this.advisoryRepository,
    required this.favoritesRepository,
    required this.fieldGuideRepository,
    required this.flowRepository,
    GoRouter? router,
  }) : router = router ?? createRouter();

  final AppConfig config;
  final VenueEventHub eventHub;
  final AttractionRepository attractionRepository;
  final AdvisoryRepository advisoryRepository;
  final FavoritesRepository favoritesRepository;
  final FieldGuideRepository fieldGuideRepository;
  final FlowRepository flowRepository;
  final GoRouter router;

  @override
  Widget build(BuildContext context) {
    return RepositoryProvider<AppConfig>.value(
      value: config,
      child: RepositoryProvider<VenueEventHub>.value(
        value: eventHub,
        child: RepositoryProvider<AttractionRepository>.value(
          value: attractionRepository,
          child: RepositoryProvider<AdvisoryRepository>.value(
            value: advisoryRepository,
            child: RepositoryProvider<FavoritesRepository>.value(
              value: favoritesRepository,
              child: RepositoryProvider<FieldGuideRepository>.value(
                value: fieldGuideRepository,
                child: RepositoryProvider<FlowRepository>.value(
                  value: flowRepository,
                  child: MultiBlocProvider(
                    providers: [
                      BlocProvider(
                        create: (context) => AttractionsBloc(
                          repository: context.read<AttractionRepository>(),
                        )..add(const AttractionsRequested()),
                      ),
                      BlocProvider(
                        create: (context) => AdvisoriesBloc(
                          repository: context.read<AdvisoryRepository>(),
                        )..add(const AdvisoriesRequested()),
                      ),
                      BlocProvider(
                        create: (context) => FavoritesBloc(
                          repository: context.read<FavoritesRepository>(),
                        )..add(const FavoritesHydrated()),
                      ),
                      BlocProvider(
                        create: (context) => FieldGuideBloc(
                          repository: context.read<FieldGuideRepository>(),
                        )..add(const FieldGuideRequested()),
                      ),
                      BlocProvider(
                        create: (context) =>
                            FlowBloc(repository: context.read<FlowRepository>())
                              ..add(const FlowRequested()),
                      ),
                    ],
                    child: VenueLifecycleBinder(
                      child: MaterialApp.router(
                        title: 'Lumen Marsh',
                        debugShowCheckedModeBanner: false,
                        theme: LumenTheme.light(),
                        darkTheme: LumenTheme.dark(),
                        routerConfig: router,
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
