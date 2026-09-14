import 'package:flutter/material.dart';

import '../../foundations/lumen_breakpoints.dart';

class LumenResponsiveScaffold extends StatelessWidget {
  const LumenResponsiveScaffold({
    super.key,
    required this.body,
    this.navigationBar,
    this.navigationRail,
  });

  final Widget body;
  final Widget? navigationBar;
  final Widget? navigationRail;

  @override
  Widget build(BuildContext context) {
    final wide = MediaQuery.sizeOf(context).width >= LumenBreakpoints.rail;
    if (wide && navigationRail != null) {
      return Scaffold(
        body: Row(
          children: [
            navigationRail!,
            const VerticalDivider(width: 1),
            Expanded(child: body),
          ],
        ),
      );
    }
    return Scaffold(body: body, bottomNavigationBar: navigationBar);
  }
}
