import 'package:equatable/equatable.dart';

import 'field_guide_category.dart';

class FieldGuideEntry extends Equatable {
  const FieldGuideEntry({
    required this.id,
    required this.title,
    required this.category,
    required this.summary,
    required this.body,
    required this.imageAsset,
    required this.imageAlt,
  });

  factory FieldGuideEntry.fromJson(Map<String, dynamic> json) {
    return FieldGuideEntry(
      id: json['id'] as String,
      title: json['title'] as String,
      category: FieldGuideCategory.fromJson(json['category'] as String),
      summary: json['summary'] as String,
      body: json['body'] as String,
      imageAsset: json['imageAsset'] as String,
      imageAlt: json['imageAlt'] as String,
    );
  }

  final String id;
  final String title;
  final FieldGuideCategory category;
  final String summary;
  final String body;
  final String imageAsset;
  final String imageAlt;

  List<String> get bodyParagraphs => body
      .split(RegExp(r'\n\s*\n'))
      .map((paragraph) => paragraph.trim())
      .where((paragraph) => paragraph.isNotEmpty)
      .toList();

  @override
  List<Object?> get props => [
    id,
    title,
    category,
    summary,
    body,
    imageAsset,
    imageAlt,
  ];
}
