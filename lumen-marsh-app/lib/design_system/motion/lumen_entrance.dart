import 'package:flutter/material.dart';
import 'package:flutter/physics.dart';

import '../foundations/lumen_motion.dart';

class LumenEntrance extends StatefulWidget {
  const LumenEntrance({
    super.key,
    required this.child,
    this.offset = 12,
    this.play = true,
  });

  final Widget child;
  final double offset;
  final bool play;

  @override
  State<LumenEntrance> createState() => _LumenEntranceState();
}

class _LumenEntranceState extends State<LumenEntrance>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController.unbounded(vsync: this)..value = 0;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted || !widget.play) {
        _controller.value = 1;
        return;
      }
      if (MediaQuery.disableAnimationsOf(context)) {
        _controller.value = 1;
        return;
      }
      _controller.animateWith(SpringSimulation(LumenSprings.settle, 0, 1, 0));
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final reduce = MediaQuery.disableAnimationsOf(context);
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, child) {
        final t = _controller.value.clamp(0.0, 1.0);
        if (reduce) {
          return Opacity(opacity: t, child: child);
        }
        return Opacity(
          opacity: t,
          child: Transform.translate(
            offset: Offset(0, widget.offset * (1 - t)),
            child: child,
          ),
        );
      },
      child: widget.child,
    );
  }
}
