import 'package:flutter/material.dart';

import '../components/status/lumen_tone.dart';
import '../foundations/lumen_colors.dart';

@immutable
class LumenColors extends ThemeExtension<LumenColors> {
  const LumenColors({
    required this.statusPositive,
    required this.onStatusPositive,
    required this.statusPositiveContainer,
    required this.onStatusPositiveContainer,
    required this.statusInformational,
    required this.onStatusInformational,
    required this.statusInformationalContainer,
    required this.onStatusInformationalContainer,
    required this.statusWarning,
    required this.onStatusWarning,
    required this.statusWarningContainer,
    required this.onStatusWarningContainer,
    required this.statusCritical,
    required this.onStatusCritical,
    required this.statusCriticalContainer,
    required this.onStatusCriticalContainer,
    required this.statusNeutral,
    required this.onStatusNeutral,
    required this.statusNeutralContainer,
    required this.onStatusNeutralContainer,
  });

  final Color statusPositive;
  final Color onStatusPositive;
  final Color statusPositiveContainer;
  final Color onStatusPositiveContainer;
  final Color statusInformational;
  final Color onStatusInformational;
  final Color statusInformationalContainer;
  final Color onStatusInformationalContainer;
  final Color statusWarning;
  final Color onStatusWarning;
  final Color statusWarningContainer;
  final Color onStatusWarningContainer;
  final Color statusCritical;
  final Color onStatusCritical;
  final Color statusCriticalContainer;
  final Color onStatusCriticalContainer;
  final Color statusNeutral;
  final Color onStatusNeutral;
  final Color statusNeutralContainer;
  final Color onStatusNeutralContainer;

  static const light = LumenColors(
    statusPositive: Color(0xFF6F9A2E),
    onStatusPositive: Colors.white,
    statusPositiveContainer: Color(0xFFDCEBB0),
    onStatusPositiveContainer: Color(0xFF35520F),
    statusInformational: LumenPalette.stormglass,
    onStatusInformational: Colors.white,
    statusInformationalContainer: Color(0xFFD4E6EB),
    onStatusInformationalContainer: Color(0xFF1F4F5C),
    statusWarning: Color(0xFFC49A32),
    onStatusWarning: LumenPalette.mangrove,
    statusWarningContainer: Color(0xFFF3E1B5),
    onStatusWarningContainer: Color(0xFF6A4B12),
    statusCritical: LumenPalette.clay,
    onStatusCritical: Colors.white,
    statusCriticalContainer: Color(0xFFF3D4CC),
    onStatusCriticalContainer: LumenPalette.clay,
    statusNeutral: Color(0xFF8A8070),
    onStatusNeutral: Colors.white,
    statusNeutralContainer: Color(0xFFDDD6C6),
    onStatusNeutralContainer: LumenPalette.cypress,
  );

  static const dark = LumenColors(
    statusPositive: Color(0xFF6F9A2E),
    onStatusPositive: LumenPalette.mangrove,
    statusPositiveContainer: Color(0x2EB8E14A),
    onStatusPositiveContainer: LumenPalette.lumenGlow,
    statusInformational: LumenPalette.stormglass,
    onStatusInformational: LumenPalette.night,
    statusInformationalContainer: Color(0x403E7A88),
    onStatusInformationalContainer: Color(0xFF9ED0DB),
    statusWarning: Color(0xFFC49A32),
    onStatusWarning: LumenPalette.night,
    statusWarningContainer: Color(0xFF3A3420),
    onStatusWarningContainer: Color(0xFFE6C56A),
    statusCritical: Color(0xFFE08A76),
    onStatusCritical: LumenPalette.night,
    statusCriticalContainer: Color(0x389B3D2A),
    onStatusCriticalContainer: Color(0xFFE08A76),
    statusNeutral: Color(0xFF8A8070),
    onStatusNeutral: LumenPalette.mist,
    statusNeutralContainer: Color(0xFF2A3331),
    onStatusNeutralContainer: Color(0xFFC5BEAA),
  );

  LumenTonePalette paletteFor(LumenTone tone) {
    return switch (tone) {
      LumenTone.positive => LumenTonePalette(
        stripe: statusPositive,
        onStripe: onStatusPositive,
        container: statusPositiveContainer,
        onContainer: onStatusPositiveContainer,
      ),
      LumenTone.informational => LumenTonePalette(
        stripe: statusInformational,
        onStripe: onStatusInformational,
        container: statusInformationalContainer,
        onContainer: onStatusInformationalContainer,
      ),
      LumenTone.warning => LumenTonePalette(
        stripe: statusWarning,
        onStripe: onStatusWarning,
        container: statusWarningContainer,
        onContainer: onStatusWarningContainer,
      ),
      LumenTone.critical => LumenTonePalette(
        stripe: statusCritical,
        onStripe: onStatusCritical,
        container: statusCriticalContainer,
        onContainer: onStatusCriticalContainer,
      ),
      LumenTone.neutral => LumenTonePalette(
        stripe: statusNeutral,
        onStripe: onStatusNeutral,
        container: statusNeutralContainer,
        onContainer: onStatusNeutralContainer,
      ),
    };
  }

  @override
  LumenColors copyWith({
    Color? statusPositive,
    Color? onStatusPositive,
    Color? statusPositiveContainer,
    Color? onStatusPositiveContainer,
    Color? statusInformational,
    Color? onStatusInformational,
    Color? statusInformationalContainer,
    Color? onStatusInformationalContainer,
    Color? statusWarning,
    Color? onStatusWarning,
    Color? statusWarningContainer,
    Color? onStatusWarningContainer,
    Color? statusCritical,
    Color? onStatusCritical,
    Color? statusCriticalContainer,
    Color? onStatusCriticalContainer,
    Color? statusNeutral,
    Color? onStatusNeutral,
    Color? statusNeutralContainer,
    Color? onStatusNeutralContainer,
  }) {
    return LumenColors(
      statusPositive: statusPositive ?? this.statusPositive,
      onStatusPositive: onStatusPositive ?? this.onStatusPositive,
      statusPositiveContainer:
          statusPositiveContainer ?? this.statusPositiveContainer,
      onStatusPositiveContainer:
          onStatusPositiveContainer ?? this.onStatusPositiveContainer,
      statusInformational: statusInformational ?? this.statusInformational,
      onStatusInformational:
          onStatusInformational ?? this.onStatusInformational,
      statusInformationalContainer:
          statusInformationalContainer ?? this.statusInformationalContainer,
      onStatusInformationalContainer:
          onStatusInformationalContainer ?? this.onStatusInformationalContainer,
      statusWarning: statusWarning ?? this.statusWarning,
      onStatusWarning: onStatusWarning ?? this.onStatusWarning,
      statusWarningContainer:
          statusWarningContainer ?? this.statusWarningContainer,
      onStatusWarningContainer:
          onStatusWarningContainer ?? this.onStatusWarningContainer,
      statusCritical: statusCritical ?? this.statusCritical,
      onStatusCritical: onStatusCritical ?? this.onStatusCritical,
      statusCriticalContainer:
          statusCriticalContainer ?? this.statusCriticalContainer,
      onStatusCriticalContainer:
          onStatusCriticalContainer ?? this.onStatusCriticalContainer,
      statusNeutral: statusNeutral ?? this.statusNeutral,
      onStatusNeutral: onStatusNeutral ?? this.onStatusNeutral,
      statusNeutralContainer:
          statusNeutralContainer ?? this.statusNeutralContainer,
      onStatusNeutralContainer:
          onStatusNeutralContainer ?? this.onStatusNeutralContainer,
    );
  }

  @override
  LumenColors lerp(ThemeExtension<LumenColors>? other, double t) {
    if (other is! LumenColors) {
      return this;
    }
    return LumenColors(
      statusPositive: Color.lerp(statusPositive, other.statusPositive, t)!,
      onStatusPositive: Color.lerp(
        onStatusPositive,
        other.onStatusPositive,
        t,
      )!,
      statusPositiveContainer: Color.lerp(
        statusPositiveContainer,
        other.statusPositiveContainer,
        t,
      )!,
      onStatusPositiveContainer: Color.lerp(
        onStatusPositiveContainer,
        other.onStatusPositiveContainer,
        t,
      )!,
      statusInformational: Color.lerp(
        statusInformational,
        other.statusInformational,
        t,
      )!,
      onStatusInformational: Color.lerp(
        onStatusInformational,
        other.onStatusInformational,
        t,
      )!,
      statusInformationalContainer: Color.lerp(
        statusInformationalContainer,
        other.statusInformationalContainer,
        t,
      )!,
      onStatusInformationalContainer: Color.lerp(
        onStatusInformationalContainer,
        other.onStatusInformationalContainer,
        t,
      )!,
      statusWarning: Color.lerp(statusWarning, other.statusWarning, t)!,
      onStatusWarning: Color.lerp(onStatusWarning, other.onStatusWarning, t)!,
      statusWarningContainer: Color.lerp(
        statusWarningContainer,
        other.statusWarningContainer,
        t,
      )!,
      onStatusWarningContainer: Color.lerp(
        onStatusWarningContainer,
        other.onStatusWarningContainer,
        t,
      )!,
      statusCritical: Color.lerp(statusCritical, other.statusCritical, t)!,
      onStatusCritical: Color.lerp(
        onStatusCritical,
        other.onStatusCritical,
        t,
      )!,
      statusCriticalContainer: Color.lerp(
        statusCriticalContainer,
        other.statusCriticalContainer,
        t,
      )!,
      onStatusCriticalContainer: Color.lerp(
        onStatusCriticalContainer,
        other.onStatusCriticalContainer,
        t,
      )!,
      statusNeutral: Color.lerp(statusNeutral, other.statusNeutral, t)!,
      onStatusNeutral: Color.lerp(onStatusNeutral, other.onStatusNeutral, t)!,
      statusNeutralContainer: Color.lerp(
        statusNeutralContainer,
        other.statusNeutralContainer,
        t,
      )!,
      onStatusNeutralContainer: Color.lerp(
        onStatusNeutralContainer,
        other.onStatusNeutralContainer,
        t,
      )!,
    );
  }
}

@immutable
class LumenTonePalette {
  const LumenTonePalette({
    required this.stripe,
    required this.onStripe,
    required this.container,
    required this.onContainer,
  });

  final Color stripe;
  final Color onStripe;
  final Color container;
  final Color onContainer;
}

extension LumenColorsContext on BuildContext {
  LumenColors get lumenColors => Theme.of(this).extension<LumenColors>()!;
}
