import 'package:flutter/material.dart';

import '../../../../design_system/components/status/lumen_fact.dart';
import '../../../../design_system/foundations/lumen_breakpoints.dart';
import '../../../../design_system/foundations/lumen_spacing.dart';
import '../../domain/attraction_detail.dart';

class AttractionFactGrid extends StatelessWidget {
  const AttractionFactGrid({super.key, required this.detail});

  final AttractionDetail detail;

  @override
  Widget build(BuildContext context) {
    final width = MediaQuery.sizeOf(context).width;
    final columns = width >= LumenBreakpoints.expanded
        ? 3
        : width >= LumenBreakpoints.rail
        ? 2
        : 1;
    final experience = detail.experience;

    final facts = [
      LumenFact(
        label: 'Duration',
        value: experience.durationLabel,
        icon: Icons.timelapse,
      ),
      LumenFact(
        label: 'Intensity',
        value: experience.intensity.label,
        icon: Icons.speed,
      ),
      LumenFact(
        label: 'Environment',
        value: experience.environment.label,
        icon: Icons.landscape_outlined,
      ),
      LumenFact(
        label: 'Accessibility',
        value: experience.accessibilitySummary,
        icon: Icons.accessible,
      ),
      LumenFact(
        label: 'Height requirement',
        value: experience.heightRequirementLabel,
        icon: Icons.height,
      ),
      if (experience.singleRiderAvailable)
        const LumenFact(
          label: 'Single rider',
          value: 'Available',
          icon: Icons.person_outline,
        ),
    ];

    if (columns == 1) {
      return Column(
        children: [
          for (var i = 0; i < facts.length; i++) ...[
            facts[i],
            if (i != facts.length - 1) const SizedBox(height: LumenSpacing.md),
          ],
        ],
      );
    }

    return GridView.count(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      crossAxisCount: columns,
      childAspectRatio: 2.6,
      mainAxisSpacing: LumenSpacing.md,
      crossAxisSpacing: LumenSpacing.md,
      children: facts,
    );
  }
}
