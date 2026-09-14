import 'package:flutter/material.dart';
import 'package:flutter/physics.dart';
import 'package:flutter/services.dart';

import '../foundations/lumen_breakpoints.dart';
import '../foundations/lumen_motion.dart';
import '../foundations/lumen_radius.dart';

class LumenPressable extends StatefulWidget {
  const LumenPressable({
    super.key,
    required this.child,
    this.onPressed,
    this.semanticLabel,
    this.borderRadius,
    this.pressedScale = 0.985,
  });

  final Widget child;
  final VoidCallback? onPressed;
  final String? semanticLabel;
  final BorderRadius? borderRadius;
  final double pressedScale;

  @override
  State<LumenPressable> createState() => _LumenPressableState();
}

class _LumenPressableState extends State<LumenPressable>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  bool _focused = false;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController.unbounded(vsync: this)..value = 1;
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _springTo(double target) {
    if (!mounted) {
      return;
    }
    if (MediaQuery.disableAnimationsOf(context)) {
      _controller.value = target;
      return;
    }
    _controller.animateWith(
      SpringSimulation(
        LumenSprings.responsive,
        _controller.value,
        target,
        _controller.velocity,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final radius = widget.borderRadius ?? LumenRadius.lgBorder;
    final enabled = widget.onPressed != null;

    return Semantics(
      button: true,
      enabled: enabled,
      label: widget.semanticLabel,
      excludeSemantics: widget.semanticLabel != null,
      child: FocusableActionDetector(
        enabled: enabled,
        onShowFocusHighlight: (focused) => setState(() => _focused = focused),
        shortcuts: const {
          SingleActivator(LogicalKeyboardKey.enter): ActivateIntent(),
          SingleActivator(LogicalKeyboardKey.space): ActivateIntent(),
        },
        actions: {
          ActivateIntent: CallbackAction<ActivateIntent>(
            onInvoke: (_) {
              widget.onPressed?.call();
              return null;
            },
          ),
        },
        child: MouseRegion(
          cursor: enabled ? SystemMouseCursors.click : MouseCursor.defer,
          child: GestureDetector(
            behavior: HitTestBehavior.opaque,
            onTapDown: enabled ? (_) => _springTo(widget.pressedScale) : null,
            onTapUp: enabled ? (_) => _springTo(1) : null,
            onTapCancel: enabled ? () => _springTo(1) : null,
            onTap: widget.onPressed,
            child: AnimatedBuilder(
              animation: _controller,
              builder: (context, child) {
                return Transform.scale(scale: _controller.value, child: child);
              },
              child: DecoratedBox(
                decoration: BoxDecoration(
                  borderRadius: radius,
                  border: _focused
                      ? Border.all(
                          color: Theme.of(context).colorScheme.primary,
                          width: 2,
                        )
                      : null,
                ),
                child: ConstrainedBox(
                  constraints: const BoxConstraints(
                    minHeight: LumenBreakpoints.minTouchTarget,
                  ),
                  child: widget.child,
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
