import 'package:flutter/material.dart';

import '../foundations/lumen_colors.dart';
import '../foundations/lumen_radius.dart';
import '../foundations/lumen_typography.dart';
import 'lumen_color_extension.dart';

abstract final class LumenTheme {
  static ThemeData light() => _theme(
    brightness: Brightness.light,
    scheme: const ColorScheme(
      brightness: Brightness.light,
      primary: LumenPalette.marshTeal,
      onPrimary: LumenPalette.mist,
      secondary: LumenPalette.secondaryLight,
      onSecondary: LumenPalette.mist,
      tertiary: LumenPalette.stormglass,
      onTertiary: Colors.white,
      error: LumenPalette.clay,
      onError: Colors.white,
      surface: LumenPalette.mist,
      onSurface: LumenPalette.mangrove,
      surfaceContainerLowest: LumenPalette.surfaceLowestLight,
      surfaceContainerLow: LumenPalette.surfaceLowLight,
      surfaceContainer: LumenPalette.surfaceLight,
      surfaceContainerHigh: LumenPalette.surfaceHighLight,
      outline: LumenPalette.outlineLight,
      outlineVariant: LumenPalette.outlineVariantLight,
    ),
    colors: LumenColors.light,
  );

  static ThemeData dark() => _theme(
    brightness: Brightness.dark,
    scheme: const ColorScheme(
      brightness: Brightness.dark,
      primary: LumenPalette.lumenGlow,
      onPrimary: LumenPalette.mangrove,
      secondary: LumenPalette.secondaryDark,
      onSecondary: LumenPalette.night,
      tertiary: LumenPalette.tertiaryDark,
      onTertiary: LumenPalette.night,
      error: LumenPalette.errorDark,
      onError: LumenPalette.night,
      surface: LumenPalette.night,
      onSurface: LumenPalette.mist,
      surfaceContainerLowest: LumenPalette.surfaceLowestDark,
      surfaceContainerLow: LumenPalette.surfaceLowDark,
      surfaceContainer: LumenPalette.surfaceDark,
      surfaceContainerHigh: LumenPalette.surfaceHighDark,
      outline: LumenPalette.outlineDark,
      outlineVariant: LumenPalette.outlineVariantDark,
    ),
    colors: LumenColors.dark,
  );

  static ThemeData _theme({
    required Brightness brightness,
    required ColorScheme scheme,
    required LumenColors colors,
  }) {
    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      colorScheme: scheme,
      textTheme: LumenTypography.textTheme(scheme),
      visualDensity: VisualDensity.standard,
      extensions: [colors],
      appBarTheme: AppBarTheme(
        backgroundColor: scheme.surface,
        foregroundColor: scheme.onSurface,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: false,
        titleTextStyle: LumenTypography.appBarTitle(scheme),
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: scheme.surfaceContainerLow,
        indicatorColor: scheme.primary.withValues(alpha: 0.16),
        labelTextStyle: WidgetStateProperty.resolveWith((states) {
          return TextStyle(
            fontSize: 12,
            fontWeight: states.contains(WidgetState.selected)
                ? FontWeight.w600
                : FontWeight.w500,
          );
        }),
      ),
      navigationRailTheme: NavigationRailThemeData(
        backgroundColor: scheme.surfaceContainerLow,
        indicatorColor: scheme.primary.withValues(alpha: 0.16),
        selectedIconTheme: IconThemeData(color: scheme.primary),
        selectedLabelTextStyle: TextStyle(
          color: scheme.primary,
          fontWeight: FontWeight.w600,
        ),
      ),
      cardTheme: CardThemeData(
        color: scheme.surfaceContainerLowest,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: LumenRadius.lgBorder,
          side: BorderSide(color: scheme.outlineVariant),
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
          shape: RoundedRectangleBorder(borderRadius: LumenRadius.mdBorder),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
          shape: RoundedRectangleBorder(borderRadius: LumenRadius.mdBorder),
        ),
      ),
    );
  }
}
