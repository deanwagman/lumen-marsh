import 'package:flutter/material.dart';

/// Raw brand palette. Components should use [LumenColors] semantic roles,
/// not these values.
abstract final class LumenPalette {
  static const mangrove = Color(0xFF072824);
  static const marshTeal = Color(0xFF1C6B62);
  static const lumenGlow = Color(0xFFB8E14A);
  static const mist = Color(0xFFF3EEE0);
  static const stormglass = Color(0xFF3E7A88);
  static const cypress = Color(0xFF5C4636);
  static const clay = Color(0xFF9B3D2A);
  static const night = Color(0xFF0A1614);

  static const secondaryLight = Color(0xFF4F6420);
  static const secondaryDark = Color(0xFF9BB56A);
  static const tertiaryDark = Color(0xFF7FB3C0);
  static const errorDark = Color(0xFFE08A76);

  static const surfaceLowestLight = Color(0xFFFAF7EF);
  static const surfaceLowLight = Color(0xFFEAE4D4);
  static const surfaceLight = Color(0xFFE3DCCA);
  static const surfaceHighLight = Color(0xFFD8D0BC);
  static const outlineLight = Color(0xFF7B8A7C);
  static const outlineVariantLight = Color(0xFFC5BEAA);

  static const surfaceLowestDark = Color(0xFF07110F);
  static const surfaceLowDark = Color(0xFF12211E);
  static const surfaceDark = Color(0xFF182825);
  static const surfaceHighDark = Color(0xFF21332F);
  static const outlineDark = Color(0xFF8B9A8C);
  static const outlineVariantDark = Color(0xFF3B4A46);
}
