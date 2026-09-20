import 'package:flutter/material.dart';

import '../../../../core/formatters/timestamp_formatter.dart';
import '../../../../design_system/components/layout/lumen_page.dart';
import '../../../../design_system/components/status/lumen_notice_banner.dart';
import '../../../../design_system/components/status/lumen_tone.dart';
import '../../../../design_system/foundations/lumen_spacing.dart';
import '../../domain/guest_wait.dart';

class WaitOutlook extends StatelessWidget {
  const WaitOutlook({super.key, required this.wait});

  final GuestWait wait;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final nowLabel =
        wait.waitOutlook ??
        (wait.postedWaitMinutes == null
            ? 'Not posted'
            : '${wait.postedWaitMinutes} minutes');
    final forecast = wait.isStale
        ? 'Not available while data is stale'
        : (wait.forecast30Minutes ?? 'Not available');

    return LumenSection(
      title: 'Wait outlook',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (wait.isStale) ...[
            const LumenNoticeBanner(
              tone: LumenTone.warning,
              title: 'Stale wait data',
              message: 'This outlook is older than five minutes. Last known values are shown until a fresh observation arrives.',
            ),
            const SizedBox(height: LumenSpacing.md),
          ],
          Text('Now: $nowLabel', style: theme.textTheme.titleMedium),
          const SizedBox(height: LumenSpacing.xxs),
          Text('In 30 minutes: $forecast', style: theme.textTheme.bodyLarge),
          const SizedBox(height: LumenSpacing.xxs),
          Text('Trend: ${wait.trend.label}', style: theme.textTheme.bodyLarge),
          const SizedBox(height: LumenSpacing.xxs),
          Text(
            'Updated: ${formatWaitAge(wait.updatedAt)}',
            style: theme.textTheme.bodySmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          if (wait.simulated) ...[
            const SizedBox(height: LumenSpacing.sm),
            Text(
              'Simulated demonstration data',
              style: theme.textTheme.labelMedium?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ],
      ),
    );
  }
}
