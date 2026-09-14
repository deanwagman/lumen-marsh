import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import 'package:lumen_marsh_app/app/shell/app_shell.dart';
import 'package:lumen_marsh_app/design_system/gallery/design_system_page.dart';
import 'package:lumen_marsh_app/features/attractions/data/attraction_repository.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/bloc/attraction_detail_bloc.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/bloc/attraction_detail_event.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/pages/attraction_detail_page.dart';
import 'package:lumen_marsh_app/features/attractions/presentation/pages/attractions_page.dart';
import 'package:lumen_marsh_app/features/advisories/presentation/pages/advisories_page.dart';
import 'package:lumen_marsh_app/features/advisories/presentation/pages/advisory_detail_page.dart';
import 'package:lumen_marsh_app/features/field_guide/presentation/pages/field_guide_detail_page.dart';
import 'package:lumen_marsh_app/features/field_guide/presentation/pages/field_guide_page.dart';
import 'package:lumen_marsh_app/features/today/presentation/pages/today_page.dart';

GoRouter createRouter({String initialLocation = '/today'}) {
  final rootNavigatorKey = GlobalKey<NavigatorState>(debugLabel: 'root');
  return GoRouter(
    navigatorKey: rootNavigatorKey,
    initialLocation: initialLocation,
    routes: [
      if (kDebugMode)
        GoRoute(
          path: '/design-system',
          parentNavigatorKey: rootNavigatorKey,
          builder: (context, state) => const DesignSystemPage(),
        ),
      GoRoute(
        path: '/advisories',
        parentNavigatorKey: rootNavigatorKey,
        builder: (context, state) => const AdvisoriesPage(),
        routes: [
          GoRoute(
            path: ':id',
            parentNavigatorKey: rootNavigatorKey,
            builder: (context, state) {
              final id = state.pathParameters['id']!;
              return AdvisoryDetailPage(advisoryId: id);
            },
          ),
        ],
      ),
      StatefulShellRoute.indexedStack(
        builder: (context, state, navigationShell) {
          return AppShell(navigationShell: navigationShell);
        },
        branches: [
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/today',
                pageBuilder: (context, state) =>
                    const NoTransitionPage(child: TodayPage()),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/attractions',
                pageBuilder: (context, state) =>
                    const NoTransitionPage(child: AttractionsPage()),
                routes: [
                  GoRoute(
                    path: ':id',
                    parentNavigatorKey: rootNavigatorKey,
                    builder: (context, state) {
                      final id = state.pathParameters['id']!;
                      return BlocProvider(
                        create: (context) => AttractionDetailBloc(
                          repository: context.read<AttractionRepository>(),
                        )..add(AttractionDetailRequested(id)),
                        child: AttractionDetailPage(attractionId: id),
                      );
                    },
                  ),
                ],
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/guide',
                pageBuilder: (context, state) =>
                    const NoTransitionPage(child: FieldGuidePage()),
                routes: [
                  GoRoute(
                    path: ':id',
                    parentNavigatorKey: rootNavigatorKey,
                    builder: (context, state) {
                      final id = state.pathParameters['id']!;
                      return FieldGuideDetailPage(entryId: id);
                    },
                  ),
                ],
              ),
            ],
          ),
        ],
      ),
    ],
  );
}
