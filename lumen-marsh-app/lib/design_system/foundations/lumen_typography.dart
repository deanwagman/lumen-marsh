import 'package:flutter/material.dart';

abstract final class LumenTypography {
  static TextTheme textTheme(ColorScheme scheme) {
    final typography = Typography.material2021();
    final base = scheme.brightness == Brightness.dark
        ? typography.white
        : typography.black;
    return base.copyWith(
      headlineMedium: base.headlineMedium?.copyWith(
        fontWeight: FontWeight.w600,
        letterSpacing: -0.3,
        color: scheme.onSurface,
      ),
      headlineSmall: base.headlineSmall?.copyWith(
        fontWeight: FontWeight.w600,
        color: scheme.onSurface,
      ),
      titleLarge: base.titleLarge?.copyWith(
        fontWeight: FontWeight.w600,
        color: scheme.onSurface,
      ),
      titleMedium: base.titleMedium?.copyWith(
        fontWeight: FontWeight.w600,
        color: scheme.onSurface,
      ),
    );
  }

  static TextStyle appBarTitle(ColorScheme scheme) {
    return TextStyle(
      fontSize: 22,
      fontWeight: FontWeight.w600,
      letterSpacing: -0.3,
      color: scheme.onSurface,
    );
  }
}
