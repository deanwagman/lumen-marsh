import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';

import '../../design_system/foundations/lumen_breakpoints.dart';
import '../../design_system/foundations/lumen_spacing.dart';
import '../../features/advisories/presentation/bloc/advisories_bloc.dart';
import '../../features/advisories/presentation/bloc/advisories_state.dart';

class AppShell extends StatelessWidget {
  const AppShell({super.key, required this.navigationShell});

  final StatefulNavigationShell navigationShell;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final useRail = constraints.maxWidth >= LumenBreakpoints.rail;
        if (useRail) {
          return _RailScaffold(navigationShell: navigationShell);
        }
        return _PhoneScaffold(navigationShell: navigationShell);
      },
    );
  }
}

class _PhoneScaffold extends StatelessWidget {
  const _PhoneScaffold({required this.navigationShell});

  final StatefulNavigationShell navigationShell;

  @override
  Widget build(BuildContext context) {
    final advisoryCount = context.select<AdvisoriesBloc, int>((bloc) {
      return switch (bloc.state) {
        AdvisoriesLoaded(:final advisories) => advisories.length,
        AdvisoriesRefreshing(:final advisories) => advisories.length,
        _ => 0,
      };
    });

    return Scaffold(
      body: navigationShell,
      bottomNavigationBar: NavigationBar(
        selectedIndex: navigationShell.currentIndex,
        onDestinationSelected: (index) {
          navigationShell.goBranch(
            index,
            initialLocation: index == navigationShell.currentIndex,
          );
        },
        destinations: [
          NavigationDestination(
            icon: Badge(
              isLabelVisible: advisoryCount > 0,
              label: Text('$advisoryCount'),
              child: const Icon(Icons.wb_twilight_outlined),
            ),
            selectedIcon: Badge(
              isLabelVisible: advisoryCount > 0,
              label: Text('$advisoryCount'),
              child: const Icon(Icons.wb_twilight),
            ),
            label: 'Today',
          ),
          const NavigationDestination(
            icon: Icon(Icons.water_outlined),
            selectedIcon: Icon(Icons.water),
            label: 'Attractions',
          ),
          const NavigationDestination(
            icon: Icon(Icons.auto_stories_outlined),
            selectedIcon: Icon(Icons.auto_stories),
            label: 'Field Guide',
          ),
        ],
      ),
    );
  }
}

class _RailScaffold extends StatelessWidget {
  const _RailScaffold({required this.navigationShell});

  final StatefulNavigationShell navigationShell;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final extended =
        MediaQuery.sizeOf(context).width >= LumenBreakpoints.expanded;
    final advisoryCount = context.select<AdvisoriesBloc, int>((bloc) {
      return switch (bloc.state) {
        AdvisoriesLoaded(:final advisories) => advisories.length,
        AdvisoriesRefreshing(:final advisories) => advisories.length,
        _ => 0,
      };
    });

    return Scaffold(
      body: Row(
        children: [
          ColoredBox(
            color: theme.colorScheme.surfaceContainerLow,
            child: Column(
              children: [
                Padding(
                  padding: EdgeInsets.fromLTRB(
                    extended ? 20 : LumenSpacing.sm,
                    LumenSpacing.lg,
                    20,
                    LumenSpacing.sm,
                  ),
                  child: Align(
                    alignment: Alignment.centerLeft,
                    child: Text(
                      extended ? 'Lumen Marsh' : 'LM',
                      style: theme.textTheme.titleLarge?.copyWith(
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ),
                Expanded(
                  child: NavigationRail(
                    extended: extended,
                    selectedIndex: navigationShell.currentIndex,
                    onDestinationSelected: (index) {
                      navigationShell.goBranch(
                        index,
                        initialLocation: index == navigationShell.currentIndex,
                      );
                    },
                    destinations: [
                      NavigationRailDestination(
                        icon: Badge(
                          isLabelVisible: advisoryCount > 0,
                          label: Text('$advisoryCount'),
                          child: const Icon(Icons.wb_twilight_outlined),
                        ),
                        selectedIcon: Badge(
                          isLabelVisible: advisoryCount > 0,
                          label: Text('$advisoryCount'),
                          child: const Icon(Icons.wb_twilight),
                        ),
                        label: const Text('Today'),
                      ),
                      const NavigationRailDestination(
                        icon: Icon(Icons.water_outlined),
                        selectedIcon: Icon(Icons.water),
                        label: Text('Attractions'),
                      ),
                      const NavigationRailDestination(
                        icon: Icon(Icons.auto_stories_outlined),
                        selectedIcon: Icon(Icons.auto_stories),
                        label: Text('Field Guide'),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const VerticalDivider(width: 1),
          Expanded(child: navigationShell),
        ],
      ),
    );
  }
}
