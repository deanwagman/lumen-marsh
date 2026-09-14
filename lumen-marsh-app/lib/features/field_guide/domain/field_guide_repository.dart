import 'field_guide_entry.dart';

abstract class FieldGuideRepository {
  Future<List<FieldGuideEntry>> list();

  Future<FieldGuideEntry?> getById(String id);
}
