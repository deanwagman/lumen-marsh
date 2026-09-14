import 'package:flutter/widgets.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import 'bloc/attractions_bloc.dart';
import 'bloc/attractions_event.dart';

class AttractionsLifecycleBinder extends StatefulWidget {
  const AttractionsLifecycleBinder({super.key, required this.child});

  final Widget child;

  @override
  State<AttractionsLifecycleBinder> createState() =>
      _AttractionsLifecycleBinderState();
}

class _AttractionsLifecycleBinderState extends State<AttractionsLifecycleBinder>
    with WidgetsBindingObserver {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      context.read<AttractionsBloc>().add(
        const AttractionsLiveReconnectRequested(),
      );
    }
  }

  @override
  Widget build(BuildContext context) => widget.child;
}
