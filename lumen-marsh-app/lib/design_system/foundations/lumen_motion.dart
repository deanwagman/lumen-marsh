import 'package:flutter/physics.dart';

abstract final class LumenSprings {
  static final responsive = SpringDescription.withDurationAndBounce(
    duration: const Duration(milliseconds: 240),
    bounce: 0.04,
  );

  static final standard = SpringDescription.withDurationAndBounce(
    duration: const Duration(milliseconds: 360),
    bounce: 0.08,
  );

  static final expressive = SpringDescription.withDurationAndBounce(
    duration: const Duration(milliseconds: 520),
    bounce: 0.14,
  );

  static final settle = SpringDescription.withDurationAndBounce(
    duration: const Duration(milliseconds: 420),
    bounce: 0,
  );
}
