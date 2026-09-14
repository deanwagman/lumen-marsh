import 'package:equatable/equatable.dart';

class AttractionMedia extends Equatable {
  const AttractionMedia({
    required this.heroUrl,
    required this.thumbnailUrl,
    required this.altText,
  });

  factory AttractionMedia.fromJson(Map<String, dynamic> json) {
    return AttractionMedia(
      heroUrl: json['heroUrl'] as String,
      thumbnailUrl: json['thumbnailUrl'] as String,
      altText: json['altText'] as String,
    );
  }

  final String heroUrl;
  final String thumbnailUrl;
  final String altText;

  @override
  List<Object?> get props => [heroUrl, thumbnailUrl, altText];
}
