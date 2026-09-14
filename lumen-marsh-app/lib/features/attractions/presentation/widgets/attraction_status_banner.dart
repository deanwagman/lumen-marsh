import 'package:flutter/material.dart';

import '../../../../core/formatters/timestamp_formatter.dart';
import '../../../../design_system/components/status/lumen_notice_banner.dart';
import '../../domain/attraction_summary.dart';
import '../attraction_tone.dart';

class AttractionStatusBanner extends StatelessWidget {
  const AttractionStatusBanner({super.key, required this.attraction});

  final AttractionSummary attraction;

  @override
  Widget build(BuildContext context) {
    return LumenNoticeBanner(
      tone: toneForAttractionStatus(attraction.status),
      icon: iconForAttractionStatus(attraction.status),
      title: attraction.status.label,
      message:
          attraction.statusMessage ?? recommendedActionFor(attraction.status),
      footer:
          '${recommendedActionFor(attraction.status)} · Updated ${formatUpdatedAt(attraction.updatedAt)}',
    );
  }
}
