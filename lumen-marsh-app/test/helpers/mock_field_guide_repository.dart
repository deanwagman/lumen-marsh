import 'package:mocktail/mocktail.dart';
import 'package:lumen_marsh_app/features/field_guide/domain/field_guide_entry.dart';
import 'package:lumen_marsh_app/features/field_guide/domain/field_guide_repository.dart';

import 'seeded_field_guide.dart';

class MockFieldGuideRepository extends Mock implements FieldGuideRepository {}

void stubFieldGuideCatalog(
  MockFieldGuideRepository repository, {
  List<FieldGuideEntry> entries = seededFieldGuideEntries,
}) {
  when(() => repository.list()).thenAnswer((_) async => entries);
  when(() => repository.getById(any())).thenAnswer((invocation) async {
    final id = invocation.positionalArguments.first as String;
    for (final entry in entries) {
      if (entry.id == id) {
        return entry;
      }
    }
    return null;
  });
}
