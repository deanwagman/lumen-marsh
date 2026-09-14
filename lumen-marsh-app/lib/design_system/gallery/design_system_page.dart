import 'package:flutter/material.dart';

import '../components/actions/lumen_icon_action.dart';
import '../components/feedback/lumen_feedback.dart';
import '../components/layout/lumen_page.dart';
import '../components/status/lumen_fact.dart';
import '../components/status/lumen_notice_banner.dart';
import '../components/status/lumen_status_badge.dart';
import '../components/status/lumen_status_stripe.dart';
import '../components/status/lumen_tone.dart';
import '../foundations/lumen_breakpoints.dart';
import '../foundations/lumen_colors.dart';
import '../foundations/lumen_motion.dart';
import '../foundations/lumen_radius.dart';
import '../foundations/lumen_spacing.dart';
import '../motion/lumen_entrance.dart';
import '../motion/lumen_pressable.dart';
import '../motion/lumen_spring_builder.dart';
import '../theme/lumen_color_extension.dart';
import '../theme/lumen_theme.dart';
import '../../features/attractions/presentation/widgets/attraction_card.dart';
import '../../features/attractions/presentation/widgets/attraction_hero.dart';
import '../../features/attractions/presentation/widgets/attraction_status_banner.dart';
import 'gallery_fixtures.dart';

class DesignSystemPage extends StatefulWidget {
  const DesignSystemPage({super.key});

  @override
  State<DesignSystemPage> createState() => _DesignSystemPageState();
}

class _DesignSystemPageState extends State<DesignSystemPage> {
  var _dark = true;
  var _reduceMotion = false;

  @override
  Widget build(BuildContext context) {
    return Theme(
      data: _dark ? LumenTheme.dark() : LumenTheme.light(),
      child: MediaQuery(
        data: MediaQuery.of(context).copyWith(disableAnimations: _reduceMotion),
        child: LumenPage(
          title: 'Design system',
          actions: [
            IconButton(
              tooltip: _dark ? 'Switch to light theme' : 'Switch to dark theme',
              onPressed: () => setState(() => _dark = !_dark),
              icon: Icon(_dark ? Icons.light_mode : Icons.dark_mode),
            ),
            IconButton(
              tooltip: _reduceMotion ? 'Enable motion' : 'Reduce motion',
              onPressed: () => setState(() => _reduceMotion = !_reduceMotion),
              icon: Icon(
                _reduceMotion
                    ? Icons.motion_photos_on
                    : Icons.motion_photos_off,
              ),
            ),
          ],
          body: Builder(
            builder: (context) {
              return ListView(
                padding: LumenSpacing.headerInsets,
                children: [
                  const _SectionTitle('Foundations'),
                  Wrap(
                    spacing: LumenSpacing.sm,
                    runSpacing: LumenSpacing.sm,
                    children: const [
                      _Swatch('Mangrove', LumenPalette.mangrove),
                      _Swatch('Marsh teal', LumenPalette.marshTeal),
                      _Swatch('Lumen glow', LumenPalette.lumenGlow),
                      _Swatch('Mist', LumenPalette.mist),
                      _Swatch('Stormglass', LumenPalette.stormglass),
                      _Swatch('Cypress', LumenPalette.cypress),
                      _Swatch('Clay', LumenPalette.clay),
                      _Swatch('Night', LumenPalette.night),
                    ],
                  ),
                  const _SectionTitle('Typography'),
                  Text(
                    'Headline medium',
                    style: Theme.of(context).textTheme.headlineMedium,
                  ),
                  Text(
                    'Title large',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  Text(
                    'Body large',
                    style: Theme.of(context).textTheme.bodyLarge,
                  ),
                  Text(
                    'Label medium',
                    style: Theme.of(context).textTheme.labelMedium,
                  ),
                  const _SectionTitle('Color roles'),
                  const _ToneRow(),
                  const _SectionTitle('Buttons and actions'),
                  Wrap(
                    spacing: LumenSpacing.sm,
                    runSpacing: LumenSpacing.sm,
                    children: [
                      FilledButton(
                        onPressed: () {},
                        child: const Text('Primary'),
                      ),
                      OutlinedButton(
                        onPressed: () {},
                        child: const Text('Secondary'),
                      ),
                      const LumenIconAction(
                        icon: Icons.refresh,
                        tooltip: 'Refresh',
                        onPressed: _noop,
                      ),
                    ],
                  ),
                  const _SectionTitle('Statuses'),
                  Wrap(
                    spacing: LumenSpacing.sm,
                    runSpacing: LumenSpacing.sm,
                    children: [
                      for (final tone in LumenTone.values)
                        LumenStatusBadge(tone: tone, label: tone.name),
                    ],
                  ),
                  const SizedBox(height: LumenSpacing.sm),
                  const SizedBox(
                    height: 48,
                    child: Row(
                      children: [
                        LumenStatusStripe(tone: LumenTone.positive),
                        Expanded(child: Text(' Status stripe')),
                      ],
                    ),
                  ),
                  const _SectionTitle('Cards and information rows'),
                  const LumenFact(label: 'Wait time', value: '25 min wait'),
                  const LumenNoticeBanner(
                    tone: LumenTone.informational,
                    title: 'Weather hold',
                    message: 'Temporarily unavailable due to nearby weather.',
                    footer: 'Seek indoor shelter until weather clears.',
                  ),
                  const _SectionTitle('Loading, empty and failure states'),
                  const SizedBox(height: 120, child: LumenLoadingState()),
                  LumenEmptyState(
                    title: 'No attractions yet',
                    message: 'The catalog is quiet right now.',
                    onRetry: _noop,
                  ),
                  LumenErrorState(
                    message: 'VenueOps timed out.',
                    onRetry: _noop,
                  ),
                  LumenOfflineState(
                    message: "Can't reach VenueOps.",
                    onRetry: _noop,
                  ),
                  const _SectionTitle('Attraction compositions'),
                  AttractionCard(
                    attraction: galleryMangroveRun,
                    onPressed: _noop,
                  ),
                  const SizedBox(height: LumenSpacing.md),
                  AttractionHero(
                    detail: galleryMangroveRunDetail,
                    apiBaseUrl: 'http://localhost:8080',
                    isFavorite: false,
                    onFavoriteToggle: _noop,
                  ),
                  const SizedBox(height: LumenSpacing.md),
                  AttractionStatusBanner(attraction: galleryWeatherHold),
                  const _SectionTitle('Motion playground'),
                  _MotionPlayground(reduceMotion: _reduceMotion),
                  const _SectionTitle('Responsive examples'),
                  Text(
                    'Current width ${MediaQuery.sizeOf(context).width.round()} · '
                    'rail at ${LumenBreakpoints.rail.round()} · '
                    'expanded at ${LumenBreakpoints.expanded.round()}',
                  ),
                  const LumenContentConstraint(
                    maxWidth: 420,
                    child: LumenNoticeBanner(
                      tone: LumenTone.warning,
                      message: 'Constrained content at 420px.',
                    ),
                  ),
                ],
              );
            },
          ),
        ),
      ),
    );
  }
}

void _noop() {}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle(this.label);

  final String label;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(
        top: LumenSpacing.lg,
        bottom: LumenSpacing.sm,
      ),
      child: Text(label, style: Theme.of(context).textTheme.titleLarge),
    );
  }
}

class _Swatch extends StatelessWidget {
  const _Swatch(this.label, this.color);

  final String label;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Container(
          width: 72,
          height: 48,
          decoration: BoxDecoration(
            color: color,
            borderRadius: LumenRadius.smBorder,
            border: Border.all(color: Theme.of(context).colorScheme.outline),
          ),
        ),
        const SizedBox(height: LumenSpacing.xxs),
        Text(label, style: Theme.of(context).textTheme.labelSmall),
      ],
    );
  }
}

class _ToneRow extends StatelessWidget {
  const _ToneRow();

  @override
  Widget build(BuildContext context) {
    final colors = context.lumenColors;
    return Wrap(
      spacing: LumenSpacing.sm,
      runSpacing: LumenSpacing.sm,
      children: [
        for (final tone in LumenTone.values)
          _Swatch(tone.name, colors.paletteFor(tone).stripe),
      ],
    );
  }
}

class _MotionPlayground extends StatefulWidget {
  const _MotionPlayground({required this.reduceMotion});

  final bool reduceMotion;

  @override
  State<_MotionPlayground> createState() => _MotionPlaygroundState();
}

class _MotionPlaygroundState extends State<_MotionPlayground> {
  var _expanded = false;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        FilledButton(
          onPressed: () => setState(() => _expanded = !_expanded),
          child: Text(_expanded ? 'Reset springs' : 'Replay springs'),
        ),
        const SizedBox(height: LumenSpacing.md),
        LumenSpringBuilder(
          value: _expanded ? 1 : 0,
          spring: LumenSprings.responsive,
          builder: (context, value) {
            return Transform.scale(
              scale: 0.9 + (0.1 * value),
              child: const LumenStatusBadge(
                tone: LumenTone.positive,
                label: 'Responsive',
              ),
            );
          },
        ),
        const SizedBox(height: LumenSpacing.sm),
        LumenSpringBuilder(
          value: _expanded ? 1 : 0,
          spring: LumenSprings.standard,
          builder: (context, value) {
            return Transform.translate(
              offset: Offset(value * 24, 0),
              child: const LumenStatusBadge(
                tone: LumenTone.warning,
                label: 'Standard',
              ),
            );
          },
        ),
        const SizedBox(height: LumenSpacing.sm),
        LumenPressable(
          onPressed: () {},
          child: const Padding(
            padding: EdgeInsets.all(LumenSpacing.md),
            child: Text('Pressable card (responsive spring)'),
          ),
        ),
        const SizedBox(height: LumenSpacing.sm),
        LumenEntrance(
          key: ValueKey(_expanded),
          child: const LumenNoticeBanner(
            tone: LumenTone.critical,
            message: 'Settle entrance — no playful bounce.',
          ),
        ),
        if (widget.reduceMotion)
          const Padding(
            padding: EdgeInsets.only(top: LumenSpacing.sm),
            child: Text('Reduced motion is on: spatial springs are disabled.'),
          ),
      ],
    );
  }
}
