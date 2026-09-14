import 'package:flutter/material.dart';

class WaitTimeDisplay extends StatelessWidget {
  const WaitTimeDisplay({
    super.key,
    required this.label,
    this.emphasized = false,
  });

  final String label;
  final bool emphasized;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Text(
      label,
      style:
          (emphasized
                  ? theme.textTheme.titleMedium
                  : theme.textTheme.bodyMedium)
              ?.copyWith(
                fontWeight: emphasized ? FontWeight.w700 : FontWeight.w500,
              ),
    );
  }
}
