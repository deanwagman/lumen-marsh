import 'package:flutter/material.dart';

import '../../../../design_system/foundations/lumen_spacing.dart';
import '../../domain/field_guide_category.dart';

class FieldGuideFilters extends StatelessWidget {
  const FieldGuideFilters({
    super.key,
    required this.selectedCategory,
    required this.onSelected,
  });

  final FieldGuideCategory? selectedCategory;
  final ValueChanged<FieldGuideCategory?> onSelected;

  @override
  Widget build(BuildContext context) {
    final options = <(FieldGuideCategory?, String)>[
      (null, 'All'),
      for (final category in FieldGuideCategory.values)
        (category, category.label),
    ];

    return SingleChildScrollView(
      key: const Key('field-guide-filters'),
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          for (var i = 0; i < options.length; i++) ...[
            ChoiceChip(
              label: Text(options[i].$2),
              selected: selectedCategory == options[i].$1,
              onSelected: (_) => onSelected(options[i].$1),
            ),
            if (i != options.length - 1) const SizedBox(width: LumenSpacing.sm),
          ],
        ],
      ),
    );
  }
}
