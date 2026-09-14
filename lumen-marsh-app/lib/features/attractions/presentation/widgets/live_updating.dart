import 'package:flutter/material.dart';

class LiveUpdating extends StatelessWidget {
  const LiveUpdating({
    super.key,
    required this.animationKey,
    required this.child,
  });

  final Object animationKey;
  final Widget child;

  static const _duration = Duration(milliseconds: 240);

  @override
  Widget build(BuildContext context) {
    final reduceMotion = MediaQuery.disableAnimationsOf(context);
    if (reduceMotion) {
      return KeyedSubtree(key: ValueKey(animationKey), child: child);
    }

    return AnimatedSwitcher(
      duration: _duration,
      switchInCurve: Curves.easeOutCubic,
      switchOutCurve: Curves.easeInCubic,
      transitionBuilder: (child, animation) {
        final offset = Tween<Offset>(
          begin: const Offset(0, 0.12),
          end: Offset.zero,
        ).animate(animation);
        return FadeTransition(
          opacity: animation,
          child: SlideTransition(position: offset, child: child),
        );
      },
      child: KeyedSubtree(key: ValueKey(animationKey), child: child),
    );
  }
}
