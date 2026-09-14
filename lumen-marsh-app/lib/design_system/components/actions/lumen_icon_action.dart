import 'package:flutter/material.dart';

import '../../foundations/lumen_breakpoints.dart';

class LumenIconAction extends StatelessWidget {
  const LumenIconAction({
    super.key,
    required this.icon,
    required this.tooltip,
    this.onPressed,
  });

  final IconData icon;
  final String tooltip;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return SizedBox.square(
      dimension: LumenBreakpoints.minTouchTarget,
      child: IconButton(
        tooltip: tooltip,
        onPressed: onPressed,
        icon: Icon(icon),
      ),
    );
  }
}
