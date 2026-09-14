import 'package:flutter/material.dart';

import '../../foundations/lumen_breakpoints.dart';
import '../../foundations/lumen_radius.dart';
import '../../foundations/lumen_spacing.dart';
import '../../theme/lumen_color_extension.dart';
import 'lumen_tone.dart';

class LumenNoticeBanner extends StatelessWidget {
  const LumenNoticeBanner({
    super.key,
    required this.tone,
    required this.message,
    this.title,
    this.icon,
    this.footer,
  });

  final LumenTone tone;
  final String message;
  final String? title;
  final IconData? icon;
  final String? footer;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final palette = context.lumenColors.paletteFor(tone);
    return Semantics(
      container: true,
      label: [title, message, footer]
          .whereType<String>()
          .map((part) => part.trim().replaceAll(RegExp(r'\.+$'), ''))
          .where((part) => part.isNotEmpty)
          .join('. '),
      excludeSemantics: true,
      child: Material(
        color: palette.container,
        borderRadius: LumenRadius.mdBorder,
        child: Padding(
          padding: const EdgeInsets.all(LumenSpacing.sm),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(
                icon ?? _iconFor(tone),
                color: palette.onContainer,
                size: LumenIconSize.md,
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (title != null)
                      Text(
                        title!,
                        style: theme.textTheme.titleSmall?.copyWith(
                          color: palette.onContainer,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    Text(
                      message,
                      style: theme.textTheme.bodyMedium?.copyWith(
                        color: palette.onContainer,
                      ),
                    ),
                    if (footer != null) ...[
                      const SizedBox(height: LumenSpacing.xxs),
                      Text(
                        footer!,
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: palette.onContainer,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  static IconData _iconFor(LumenTone tone) {
    return switch (tone) {
      LumenTone.positive => Icons.check_circle_outline,
      LumenTone.neutral => Icons.info_outline,
      LumenTone.informational => Icons.cloud_outlined,
      LumenTone.warning => Icons.warning_amber_outlined,
      LumenTone.critical => Icons.error_outline,
    };
  }
}
