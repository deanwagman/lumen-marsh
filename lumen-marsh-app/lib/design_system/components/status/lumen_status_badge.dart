import 'package:flutter/material.dart';

import '../../foundations/lumen_radius.dart';
import '../../foundations/lumen_spacing.dart';
import '../../theme/lumen_color_extension.dart';
import 'lumen_tone.dart';

class LumenStatusBadge extends StatelessWidget {
  const LumenStatusBadge({super.key, required this.tone, required this.label});

  final LumenTone tone;
  final String label;

  @override
  Widget build(BuildContext context) {
    final palette = context.lumenColors.paletteFor(tone);
    return Semantics(
      label: label,
      excludeSemantics: true,
      child: Container(
        padding: const EdgeInsets.symmetric(
          horizontal: 10,
          vertical: LumenSpacing.xxs + 2,
        ),
        decoration: BoxDecoration(
          color: palette.container,
          borderRadius: LumenRadius.pillBorder,
        ),
        child: Text(
          label,
          style: Theme.of(context).textTheme.labelMedium?.copyWith(
            color: palette.onContainer,
            fontWeight: FontWeight.w600,
          ),
        ),
      ),
    );
  }
}
