import 'package:equatable/equatable.dart';

import 'attraction_enums.dart';
import 'attraction_experience.dart';
import 'attraction_summary.dart';

class AttractionDetail extends Equatable with AttractionOperationalFields {
  const AttractionDetail({required this.summary, required this.experience});

  factory AttractionDetail.fromJson(Map<String, dynamic> json) {
    return AttractionDetail(
      summary: AttractionSummary.fromJson(json),
      experience: AttractionExperience.fromJson(
        Map<String, dynamic>.from(json['experience'] as Map),
      ),
    );
  }

  final AttractionSummary summary;
  final AttractionExperience experience;

  @override
  String get id => summary.id;
  @override
  String get name => summary.name;
  @override
  String get area => summary.area;
  @override
  AttractionType get type => summary.type;
  @override
  AttractionStatus get status => summary.status;
  @override
  AttractionCapacityMode get capacityMode => summary.capacityMode;
  @override
  int? get waitMinutes => summary.waitMinutes;
  @override
  String? get statusMessage => summary.statusMessage;
  @override
  DateTime get updatedAt => summary.updatedAt;
  @override
  int get version => summary.version;

  @override
  List<Object?> get props => [summary, experience];

  AttractionDetail withSummary(AttractionSummary summary) {
    return AttractionDetail(summary: summary, experience: experience);
  }
}
