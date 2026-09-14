import 'package:lumen_marsh_app/features/field_guide/domain/field_guide_category.dart';
import 'package:lumen_marsh_app/features/field_guide/domain/field_guide_entry.dart';

const ghostOrchid = FieldGuideEntry(
  id: 'ghost-orchid',
  title: 'Lumen Ghost Orchid',
  category: FieldGuideCategory.flora,
  summary: 'A nocturnal orchid observed near Mangrove Run.',
  body: 'Field observers first logged the Lumen Ghost Orchid along the quieter eddies of Mangrove Run.\n\nGuests are asked not to touch the blooms.',
  imageAsset: 'assets/field_guide/images/ghost-orchid.png',
  imageAlt: 'A pale orchid glowing beneath mangrove roots.',
);

const biolumeMoss = FieldGuideEntry(
  id: 'biolume-moss',
  title: 'Biolume Boardwalk Moss',
  category: FieldGuideCategory.flora,
  summary:
      'Soft moss that brightens footfalls on Luminous Wetlands boardwalks.',
  body:
      'Biolume moss colonizes shaded planks throughout the Luminous Wetlands.',
  imageAsset: 'assets/field_guide/images/biolume-moss.png',
  imageAlt: 'Soft green moss glowing along a wooden boardwalk plank.',
);

const marshHeron = FieldGuideEntry(
  id: 'marsh-heron',
  title: 'Cypress Basin Dusk Heron',
  category: FieldGuideCategory.wildlife,
  summary: 'A dusk heron that hunts the still water around Cypress Coil.',
  body: 'The Cypress Basin dusk heron arrives as Cypress Coil quietens for evening.',
  imageAsset: 'assets/field_guide/images/marsh-heron.png',
  imageAlt: 'A slender heron standing in dusk water among cypress knees.',
);

const stormglassNotes = FieldGuideEntry(
  id: 'stormglass-notes',
  title: 'Stormglass Station Briefing',
  category: FieldGuideCategory.researchNotes,
  summary:
      'Research Quarter notes on how stormglass instruments read the marsh.',
  body: 'Stormglass Station anchors the Research Quarter guest-facing weather lab.',
  imageAsset: 'assets/field_guide/images/stormglass-notes.png',
  imageAlt: 'Glass weather instruments glowing inside a research station.',
);

const mangroveLore = FieldGuideEntry(
  id: 'mangrove-lore',
  title: 'Mangrove Run Expedition Lore',
  category: FieldGuideCategory.attractionLore,
  summary: 'Why Mangrove Run boats travel beneath a living canopy.',
  body: 'Mangrove Run began as a survey route for wetland ecologists.',
  imageAsset: 'assets/field_guide/images/mangrove-lore.png',
  imageAlt: 'An expedition boat gliding beneath a glowing mangrove canopy.',
);

const coilOrigin = FieldGuideEntry(
  id: 'coil-origin',
  title: 'Cypress Coil Origin Note',
  category: FieldGuideCategory.attractionLore,
  summary: 'How a research launch track became Cypress Coil.',
  body: 'Cypress Coil began as a short research launch for sensor drones.',
  imageAsset: 'assets/field_guide/images/coil-origin.png',
  imageAlt: 'A launch coaster track curving through a cypress basin canopy.',
);

const seededFieldGuideEntries = [
  ghostOrchid,
  biolumeMoss,
  marshHeron,
  stormglassNotes,
  mangroveLore,
  coilOrigin,
];

const seededFieldGuideJson = '''
[
  {
    "id": "ghost-orchid",
    "title": "Lumen Ghost Orchid",
    "category": "flora",
    "summary": "A nocturnal orchid observed near Mangrove Run.",
    "body": "Paragraph one.\\n\\nParagraph two.",
    "imageAsset": "assets/field_guide/images/ghost-orchid.png",
    "imageAlt": "A pale orchid glowing beneath mangrove roots."
  },
  {
    "id": "marsh-heron",
    "title": "Cypress Basin Dusk Heron",
    "category": "wildlife",
    "summary": "A dusk heron that hunts the still water around Cypress Coil.",
    "body": "Heron notes.",
    "imageAsset": "assets/field_guide/images/marsh-heron.png",
    "imageAlt": "A slender heron standing in dusk water among cypress knees."
  }
]
''';
