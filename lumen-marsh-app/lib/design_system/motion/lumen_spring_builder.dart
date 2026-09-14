import 'package:flutter/material.dart';
import 'package:flutter/physics.dart';

import '../foundations/lumen_motion.dart';

class LumenSpringBuilder extends StatefulWidget {
  const LumenSpringBuilder({
    super.key,
    required this.value,
    required this.builder,
    this.spring,
  });

  final double value;
  final SpringDescription? spring;
  final Widget Function(BuildContext context, double value) builder;

  @override
  State<LumenSpringBuilder> createState() => _LumenSpringBuilderState();
}

class _LumenSpringBuilderState extends State<LumenSpringBuilder>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController.unbounded(vsync: this)
      ..value = widget.value;
  }

  @override
  void didUpdateWidget(LumenSpringBuilder oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.value != widget.value) {
      _animateTo(widget.value);
    }
  }

  void _animateTo(double target) {
    if (MediaQuery.disableAnimationsOf(context)) {
      _controller.value = target;
      return;
    }
    _controller.animateWith(
      SpringSimulation(
        widget.spring ?? LumenSprings.standard,
        _controller.value,
        target,
        _controller.velocity,
      ),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, _) => widget.builder(context, _controller.value),
    );
  }
}
