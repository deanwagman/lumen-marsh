import 'package:flutter/material.dart';

import '../../../../design_system/components/status/lumen_status_badge.dart';
import '../../domain/attraction.dart';
import '../attraction_tone.dart';

class AttractionStatusBadge extends StatelessWidget {
  const AttractionStatusBadge({super.key, required this.status});

  final AttractionStatus status;

  @override
  Widget build(BuildContext context) {
    return LumenStatusBadge(
      tone: toneForAttractionStatus(status),
      label: status.label,
    );
  }
}
