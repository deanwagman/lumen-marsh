import 'package:flutter/material.dart';

import '../../../../core/formatters/timestamp_formatter.dart';
import '../../../../design_system/components/status/lumen_notice_banner.dart';
import '../../../../design_system/components/status/lumen_tone.dart';
import '../../../../design_system/foundations/lumen_spacing.dart';
import '../../domain/advisory_severity.dart';
import '../../domain/guest_advisory.dart';

LumenTone toneForAdvisorySeverity(AdvisorySeverity severity) {
  return switch (severity) {
    AdvisorySeverity.critical => LumenTone.critical,
    AdvisorySeverity.major => LumenTone.warning,
    AdvisorySeverity.minor => LumenTone.informational,
    AdvisorySeverity.advisory => LumenTone.informational,
  };
}

String labelForAttractionId(String id) {
  return switch (id) {
    'mangrove-run' => 'Mangrove Run',
    'cypress-coil' => 'Cypress Coil',
    'stormglass-station' => 'Stormglass Station',
    _ => id,
  };
}

/// Shared guest-safe advisory presentation used by Today, inbox, and detail.
class AdvisoryContent extends StatelessWidget {
  const AdvisoryContent({
    super.key,
    required this.advisory,
    this.compact = false,
    this.onPressed,
    this.onAttractionPressed,
  });

  final GuestAdvisory advisory;
  final bool compact;
  final VoidCallback? onPressed;
  final void Function(String attractionId)? onAttractionPressed;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final tone = toneForAdvisorySeverity(advisory.severity);
    final assertive = advisory.severity.index >= AdvisorySeverity.major.index;

    final body = Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        LumenNoticeBanner(
          tone: tone,
          title: advisory.title,
          message: advisory.message,
          footer:
              '${advisory.severity.label} · Updated ${formatUpdatedAt(advisory.updatedAt)}',
        ),
        if (!compact && advisory.affectedAttractionIds.isNotEmpty) ...[
          const SizedBox(height: LumenSpacing.sm),
          Wrap(
            spacing: LumenSpacing.xs,
            runSpacing: LumenSpacing.xs,
            children: [
              for (final id in advisory.affectedAttractionIds)
                ActionChip(
                  label: Text(labelForAttractionId(id)),
                  onPressed: onAttractionPressed == null
                      ? null
                      : () => onAttractionPressed!(id),
                ),
            ],
          ),
        ],
        if (compact && advisory.affectedAttractionIds.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(top: LumenSpacing.xxs),
            child: Text(
              'Affects ${labelForAttractionId(advisory.affectedAttractionIds.first)}'
              '${advisory.affectedAttractionIds.length > 1 ? ' and others' : ''}',
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ),
      ],
    );

    return Semantics(
      liveRegion: assertive,
      container: true,
      button: onPressed != null,
      label:
          '${advisory.severity.label} advisory. ${advisory.title}. ${advisory.message}',
      child: onPressed == null
          ? body
          : Material(
              color: Colors.transparent,
              child: InkWell(
                onTap: onPressed,
                borderRadius: BorderRadius.circular(12),
                child: body,
              ),
            ),
    );
  }
}

class GuestAdvisoryBanner extends StatelessWidget {
  const GuestAdvisoryBanner({
    super.key,
    required this.advisory,
    this.compact = false,
    this.onPressed,
    this.onAttractionPressed,
  });

  final GuestAdvisory advisory;
  final bool compact;
  final VoidCallback? onPressed;
  final void Function(String attractionId)? onAttractionPressed;

  @override
  Widget build(BuildContext context) {
    return AdvisoryContent(
      advisory: advisory,
      compact: compact,
      onPressed: onPressed,
      onAttractionPressed: onAttractionPressed,
    );
  }
}

class AdvisoryClearedBanner extends StatelessWidget {
  const AdvisoryClearedBanner({super.key, required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return LumenNoticeBanner(tone: LumenTone.informational, message: message);
  }
}
