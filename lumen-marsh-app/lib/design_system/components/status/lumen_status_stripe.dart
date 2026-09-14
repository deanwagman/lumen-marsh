import 'package:flutter/material.dart';

import '../../foundations/lumen_breakpoints.dart';
import '../../theme/lumen_color_extension.dart';
import 'lumen_tone.dart';

class LumenStatusStripe extends StatelessWidget {
  const LumenStatusStripe({super.key, required this.tone});

  final LumenTone tone;

  @override
  Widget build(BuildContext context) {
    return ExcludeSemantics(
      child: ColoredBox(
        color: context.lumenColors.paletteFor(tone).stripe,
        child: const SizedBox(width: LumenBreakpoints.statusStripeWidth),
      ),
    );
  }
}
