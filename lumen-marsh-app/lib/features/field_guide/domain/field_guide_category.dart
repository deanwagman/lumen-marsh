enum FieldGuideCategory {
  flora,
  wildlife,
  researchNotes,
  attractionLore;

  String get label => switch (this) {
    FieldGuideCategory.flora => 'Flora',
    FieldGuideCategory.wildlife => 'Wildlife',
    FieldGuideCategory.researchNotes => 'Research Notes',
    FieldGuideCategory.attractionLore => 'Attraction Lore',
  };

  static FieldGuideCategory fromJson(String value) => switch (value) {
    'flora' => FieldGuideCategory.flora,
    'wildlife' => FieldGuideCategory.wildlife,
    'researchNotes' => FieldGuideCategory.researchNotes,
    'attractionLore' => FieldGuideCategory.attractionLore,
    _ => throw FormatException('Unknown field guide category: $value'),
  };

  String toJson() => name;
}
