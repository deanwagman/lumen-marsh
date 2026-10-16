import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

/// Opens the in-app guide for the live board on Today.
class BoardGuideLink extends StatelessWidget {
  const BoardGuideLink({
    super.key,
    required this.sectionId,
    required this.label,
  });

  final String sectionId;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.centerLeft,
      child: TextButton(
        onPressed: () => context.push('/today/board?section=$sectionId'),
        child: Text(label),
      ),
    );
  }
}
