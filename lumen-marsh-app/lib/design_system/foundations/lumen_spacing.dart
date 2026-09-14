import 'package:flutter/material.dart';

abstract final class LumenSpacing {
  static const xxs = 4.0;
  static const xs = 8.0;
  static const sm = 12.0;
  static const md = 16.0;
  static const lg = 24.0;
  static const xl = 32.0;
  static const xxl = 48.0;

  static const pageInsets = EdgeInsets.fromLTRB(20, 8, 20, 28);
  static const headerInsets = EdgeInsets.fromLTRB(20, 12, 20, 32);
  static const feedbackInsets = EdgeInsets.all(24);
}
