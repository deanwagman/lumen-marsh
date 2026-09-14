import 'package:flutter/material.dart';

import '../../foundations/lumen_breakpoints.dart';
import '../../foundations/lumen_spacing.dart';
import '../layout/lumen_page.dart';

class LumenLoadingState extends StatelessWidget {
  const LumenLoadingState({super.key, this.message = 'Reading the marsh…'});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const CircularProgressIndicator(),
          const SizedBox(height: LumenSpacing.md),
          Text(message, style: Theme.of(context).textTheme.bodyLarge),
        ],
      ),
    );
  }
}

class LumenEmptyState extends StatelessWidget {
  const LumenEmptyState({
    super.key,
    required this.title,
    required this.message,
    this.onRetry,
    this.actionLabel = 'Refresh',
    this.icon = Icons.water_drop_outlined,
  });

  final String title;
  final String message;
  final VoidCallback? onRetry;
  final String actionLabel;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return _FeedbackPanel(
      icon: icon,
      iconColor: Theme.of(context).colorScheme.primary,
      title: title,
      message: message,
      onRetry: onRetry,
      actionLabel: actionLabel,
    );
  }
}

class LumenErrorState extends StatelessWidget {
  const LumenErrorState({
    super.key,
    required this.message,
    this.title = "VenueOps couldn't load this view",
    this.onRetry,
    this.actionLabel = 'Try again',
  });

  final String title;
  final String message;
  final VoidCallback? onRetry;
  final String actionLabel;

  @override
  Widget build(BuildContext context) {
    return _FeedbackPanel(
      icon: Icons.error_outline,
      iconColor: Theme.of(context).colorScheme.error,
      title: title,
      message: message,
      onRetry: onRetry,
      actionLabel: actionLabel,
    );
  }
}

class LumenOfflineState extends StatelessWidget {
  const LumenOfflineState({
    super.key,
    required this.message,
    this.onRetry,
    this.actionLabel = 'Try again',
  });

  final String message;
  final VoidCallback? onRetry;
  final String actionLabel;

  @override
  Widget build(BuildContext context) {
    return _FeedbackPanel(
      icon: Icons.wifi_off,
      iconColor: Theme.of(context).colorScheme.tertiary,
      title: "You're offline from the marsh",
      message: message,
      onRetry: onRetry,
      actionLabel: actionLabel,
    );
  }
}

class _FeedbackPanel extends StatelessWidget {
  const _FeedbackPanel({
    required this.icon,
    required this.iconColor,
    required this.title,
    required this.message,
    required this.actionLabel,
    this.onRetry,
  });

  final IconData icon;
  final Color iconColor;
  final String title;
  final String message;
  final String actionLabel;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return LumenContentConstraint(
      maxWidth: LumenBreakpoints.feedbackMaxWidth,
      alignment: Alignment.center,
      child: Padding(
        padding: LumenSpacing.feedbackInsets,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: LumenIconSize.lg, color: iconColor),
            const SizedBox(height: LumenSpacing.md),
            Text(
              title,
              textAlign: TextAlign.center,
              style: theme.textTheme.headlineSmall,
            ),
            const SizedBox(height: LumenSpacing.xs),
            Text(
              message,
              textAlign: TextAlign.center,
              style: theme.textTheme.bodyLarge?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            if (onRetry != null) ...[
              const SizedBox(height: 20),
              FilledButton(onPressed: onRetry, child: Text(actionLabel)),
            ],
          ],
        ),
      ),
    );
  }
}
