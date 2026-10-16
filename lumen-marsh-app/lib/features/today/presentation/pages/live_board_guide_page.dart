import 'package:flutter/material.dart';

import '../../../../design_system/components/layout/lumen_page.dart';
import '../../../../design_system/foundations/lumen_breakpoints.dart';
import '../../../../design_system/foundations/lumen_spacing.dart';

class LiveBoardSection {
  const LiveBoardSection({
    required this.id,
    required this.title,
    required this.paragraphs,
  });

  final String id;
  final String title;
  final List<String> paragraphs;
}

/// Guest copy for the live board. Kept beside the page that renders it.
const liveBoardSections = <LiveBoardSection>[
  LiveBoardSection(
    id: 'waits',
    title: 'What "About 25 minutes" means',
    paragraphs: [
      'A wait like "About 25 minutes" is the park\'s posted wait for that queue, rounded to the nearest five minutes. Use it to plan. It is not a place in line, and it is not a reservation.',
      'It changes when the park updates the posted wait. It does not tick down on its own while you walk.',
      'A ride card may show the same wait as "25 min wait." That line is the posted wait in whole minutes. "About 25 minutes" is that posted wait, rounded, so the two can differ by a few minutes.',
      '"In 30 minutes" uses the same "About … minutes" words for a look ahead. That look ahead drops off when the wait picture is older than five minutes.',
      'The park keeps a finer clock for the people running the ride. This phrase is the planning number on your board, not that clock.',
    ],
  ),
  LiveBoardSection(
    id: 'best-next',
    title: 'Why a suggestion appears',
    paragraphs: [
      'Best Next lists open rides from the board the park has published. It orders them using the posted wait, a 30-minute outlook when that outlook is fresh, the walk from the zone you choose, and rides you have saved.',
      'A published note can move a ride up the list. When that note recommends the ride, its message is the reason you see.',
      'If a published note says a ride is unavailable, that ride comes off the list, even when the ride still looks open.',
      'The suggestion follows that published board. It does not explain anything the park has not published here.',
      'Once suggestions are on screen, a wait picture older than five minutes keeps that order until a fresh picture arrives.',
    ],
  ),
  LiveBoardSection(
    id: 'still-open',
    title: 'Why a ride can still look open',
    paragraphs: [
      'Lightning, a ride the park is checking, or a line that is backing up can already be known behind the scenes while this app still shows the ride as open.',
      'What you see changes when the park publishes a note for guests, or when it changes the ride\'s status on this board. Until then, this app shows the last picture the park published.',
      'A 30-minute outlook can refresh before that picture changes. The ride still shows as open until the park changes its status here, or publishes a note that takes it off Best Next.',
      'Tools can recommend a change. People at the park decide. You see the picture they publish.',
    ],
  ),
];

class LiveBoardGuidePage extends StatefulWidget {
  const LiveBoardGuidePage({super.key, this.sectionId});

  final String? sectionId;

  @override
  State<LiveBoardGuidePage> createState() => _LiveBoardGuidePageState();
}

class _LiveBoardGuidePageState extends State<LiveBoardGuidePage> {
  final _sectionKeys = {
    for (final section in liveBoardSections) section.id: GlobalKey(),
  };

  @override
  void initState() {
    super.initState();
    _reveal(widget.sectionId);
  }

  @override
  void didUpdateWidget(LiveBoardGuidePage oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.sectionId != widget.sectionId) {
      _reveal(widget.sectionId);
    }
  }

  void _reveal(String? sectionId) {
    if (sectionId == null || sectionId.isEmpty) {
      return;
    }
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) {
        return;
      }
      final target = _sectionKeys[sectionId]?.currentContext;
      if (target == null) {
        return;
      }
      final animationsDisabled = MediaQuery.disableAnimationsOf(context);
      Scrollable.ensureVisible(
        target,
        alignment: 0.02,
        duration: animationsDisabled
            ? Duration.zero
            : const Duration(milliseconds: 240),
        curve: Curves.easeOutCubic,
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return LumenPage(
      title: 'How the board works',
      body: LumenContentConstraint(
        maxWidth: LumenBreakpoints.pageMaxWidth,
        child: SingleChildScrollView(
          key: const Key('live-board-guide'),
          padding: LumenSpacing.pageInsets,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Today shows the live board: waits, Best Next, and which rides are open. This page is how to read it. The field guide stays the story of the park.',
                style: theme.textTheme.bodyLarge?.copyWith(
                  color: theme.colorScheme.onSurfaceVariant,
                ),
              ),
              const SizedBox(height: LumenSpacing.lg),
              Text('On this page', style: theme.textTheme.titleMedium),
              for (final section in liveBoardSections)
                Align(
                  alignment: Alignment.centerLeft,
                  child: TextButton(
                    key: Key('live-board-toc-${section.id}'),
                    onPressed: () => _reveal(section.id),
                    child: Text(section.title),
                  ),
                ),
              const SizedBox(height: LumenSpacing.lg),
              for (final section in liveBoardSections) ...[
                _SectionBody(key: _sectionKeys[section.id], section: section),
                const SizedBox(height: LumenSpacing.xl),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _SectionBody extends StatelessWidget {
  const _SectionBody({super.key, required this.section});

  final LiveBoardSection section;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      key: Key('live-board-section-${section.id}'),
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Semantics(
          header: true,
          child: Text(section.title, style: theme.textTheme.titleLarge),
        ),
        const SizedBox(height: LumenSpacing.md),
        for (var i = 0; i < section.paragraphs.length; i++) ...[
          Text(section.paragraphs[i], style: theme.textTheme.bodyLarge),
          if (i != section.paragraphs.length - 1)
            const SizedBox(height: LumenSpacing.md),
        ],
      ],
    );
  }
}
