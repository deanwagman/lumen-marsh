import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../design_system/components/feedback/lumen_feedback.dart';
import '../../../../design_system/components/layout/lumen_page.dart';
import '../../../../design_system/foundations/lumen_breakpoints.dart';
import '../../../../design_system/foundations/lumen_spacing.dart';
import '../../domain/field_guide_category.dart';
import '../../domain/field_guide_entry.dart';
import '../bloc/field_guide_bloc.dart';
import '../bloc/field_guide_event.dart';
import '../bloc/field_guide_state.dart';
import '../widgets/field_guide_entry_card.dart';
import '../widgets/field_guide_filters.dart';

class FieldGuidePage extends StatefulWidget {
  const FieldGuidePage({super.key});

  @override
  State<FieldGuidePage> createState() => _FieldGuidePageState();
}

class _FieldGuidePageState extends State<FieldGuidePage> {
  late final TextEditingController _searchController;

  @override
  void initState() {
    super.initState();
    final state = context.read<FieldGuideBloc>().state;
    _searchController = TextEditingController(
      text: state is FieldGuideLoaded ? state.query : '',
    );
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return LumenPage(
      title: 'Field Guide',
      body: BlocConsumer<FieldGuideBloc, FieldGuideState>(
        listenWhen: (previous, current) {
          if (current is! FieldGuideLoaded) {
            return false;
          }
          if (previous is! FieldGuideLoaded) {
            return true;
          }
          return previous.query != current.query;
        },
        listener: (context, state) {
          if (state is FieldGuideLoaded &&
              _searchController.text != state.query) {
            _searchController.value = TextEditingValue(
              text: state.query,
              selection: TextSelection.collapsed(offset: state.query.length),
            );
          }
        },
        builder: (context, state) {
          return switch (state) {
            FieldGuideInitial() || FieldGuideLoading() =>
              const LumenLoadingState(message: 'Opening the field journal…'),
            FieldGuideLoaded(
              :final entries,
              :final visibleEntries,
              :final selectedCategory,
              :final query,
            ) =>
              entries.isEmpty
                  ? LumenEmptyState(
                      title: 'Field Guide is quiet',
                      message: 'Explorer notes have not loaded yet. Try refreshing the journal.',
                      actionLabel: 'Reload',
                      icon: Icons.auto_stories_outlined,
                      onRetry: () => context.read<FieldGuideBloc>().add(
                        const FieldGuideRequested(),
                      ),
                    )
                  : _FieldGuideCatalog(
                      searchController: _searchController,
                      selectedCategory: selectedCategory,
                      query: query,
                      visibleEntries: visibleEntries,
                    ),
          };
        },
      ),
    );
  }
}

class _FieldGuideCatalog extends StatelessWidget {
  const _FieldGuideCatalog({
    required this.searchController,
    required this.selectedCategory,
    required this.query,
    required this.visibleEntries,
  });

  final TextEditingController searchController;
  final FieldGuideCategory? selectedCategory;
  final String query;
  final List<FieldGuideEntry> visibleEntries;

  @override
  Widget build(BuildContext context) {
    final width = MediaQuery.sizeOf(context).width;
    final crossAxisCount = width >= LumenBreakpoints.expanded
        ? 3
        : width >= LumenBreakpoints.rail
        ? 2
        : 1;

    return CustomScrollView(
      key: const Key('field-guide-catalog'),
      slivers: [
        SliverPadding(
          padding: LumenSpacing.pageInsets,
          sliver: SliverToBoxAdapter(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const LumenPageHeader(
                  title: 'Field journal',
                  subtitle: 'Flora, wildlife, research notes, and attraction lore from across Lumen Marsh.',
                ),
                const SizedBox(height: LumenSpacing.lg),
                TextField(
                  key: const Key('field-guide-search'),
                  controller: searchController,
                  textInputAction: TextInputAction.search,
                  decoration: InputDecoration(
                    labelText: 'Search field guide',
                    hintText: 'Title, summary, or notes',
                    prefixIcon: const Icon(Icons.search),
                    suffixIcon: query.trim().isEmpty
                        ? null
                        : IconButton(
                            tooltip: 'Clear search',
                            onPressed: () {
                              searchController.clear();
                              context.read<FieldGuideBloc>().add(
                                const FieldGuideSearchChanged(''),
                              );
                            },
                            icon: const Icon(Icons.clear),
                          ),
                    border: const OutlineInputBorder(),
                  ),
                  onChanged: (value) => context.read<FieldGuideBloc>().add(
                    FieldGuideSearchChanged(value),
                  ),
                ),
                const SizedBox(height: LumenSpacing.md),
                FieldGuideFilters(
                  selectedCategory: selectedCategory,
                  onSelected: (category) => context.read<FieldGuideBloc>().add(
                    FieldGuideCategorySelected(category),
                  ),
                ),
                const SizedBox(height: LumenSpacing.lg),
              ],
            ),
          ),
        ),
        if (visibleEntries.isEmpty)
          const SliverToBoxAdapter(child: SizedBox(height: LumenSpacing.md)),
        if (visibleEntries.isEmpty)
          SliverToBoxAdapter(
            child: Padding(
              padding: LumenSpacing.pageInsets,
              child: LumenEmptyState(
                title: 'No entries match',
                message: 'Try another search term or clear filters to browse the full journal.',
                actionLabel: 'Clear filters',
                icon: Icons.search_off,
                onRetry: () {
                  searchController.clear();
                  context.read<FieldGuideBloc>().add(
                    const FieldGuideFiltersCleared(),
                  );
                },
              ),
            ),
          )
        else
          SliverPadding(
            padding: LumenSpacing.pageInsets.copyWith(top: 0),
            sliver: SliverGrid(
              gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: crossAxisCount,
                mainAxisSpacing: LumenSpacing.md,
                crossAxisSpacing: LumenSpacing.md,
                childAspectRatio: crossAxisCount == 1 ? 1.2 : 0.85,
              ),
              delegate: SliverChildBuilderDelegate((context, index) {
                final entry = visibleEntries[index];
                return FieldGuideEntryCard(entry: entry);
              }, childCount: visibleEntries.length),
            ),
          ),
      ],
    );
  }
}
