import 'package:flutter/material.dart';

import '../../foundations/lumen_breakpoints.dart';

class LumenContentConstraint extends StatelessWidget {
  const LumenContentConstraint({
    super.key,
    required this.child,
    this.maxWidth = LumenBreakpoints.pageMaxWidth,
    this.alignment = Alignment.topCenter,
  });

  final Widget child;
  final double maxWidth;
  final Alignment alignment;

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: alignment,
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: maxWidth),
        child: child,
      ),
    );
  }
}

class LumenPage extends StatelessWidget {
  const LumenPage({
    super.key,
    required this.title,
    required this.body,
    this.actions,
    this.automaticallyImplyLeading = true,
  });

  final String title;
  final Widget body;
  final List<Widget>? actions;
  final bool automaticallyImplyLeading;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(title),
        actions: actions,
        automaticallyImplyLeading: automaticallyImplyLeading,
      ),
      body: body,
    );
  }
}

class LumenPageHeader extends StatelessWidget {
  const LumenPageHeader({super.key, required this.title, this.subtitle});

  final String title;
  final String? subtitle;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: theme.textTheme.headlineMedium),
        if (subtitle != null) ...[
          const SizedBox(height: 8),
          Text(
            subtitle!,
            style: theme.textTheme.bodyLarge?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        ],
      ],
    );
  }
}

class LumenSection extends StatelessWidget {
  const LumenSection({
    super.key,
    required this.child,
    this.title,
    this.spacing = 16,
  });

  final String? title;
  final Widget child;
  final double spacing;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (title != null) ...[
          Text(title!, style: Theme.of(context).textTheme.titleLarge),
          SizedBox(height: spacing),
        ],
        child,
      ],
    );
  }
}
