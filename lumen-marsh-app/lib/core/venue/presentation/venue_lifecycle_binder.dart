import 'package:flutter/widgets.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../features/attractions/presentation/bloc/attractions_bloc.dart';
import '../../../features/attractions/presentation/bloc/attractions_event.dart';
import '../../../features/advisories/presentation/bloc/advisories_bloc.dart';
import '../../../features/advisories/presentation/bloc/advisories_event.dart';
import '../../../features/flow/presentation/bloc/flow_bloc.dart';
import '../../../features/flow/presentation/bloc/flow_event.dart';

class VenueLifecycleBinder extends StatefulWidget {
  const VenueLifecycleBinder({super.key, required this.child});

  final Widget child;

  @override
  State<VenueLifecycleBinder> createState() => _VenueLifecycleBinderState();
}

class _VenueLifecycleBinderState extends State<VenueLifecycleBinder>
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
      context.read<AdvisoriesBloc>().add(
        const AdvisoriesLiveReconnectRequested(),
      );
      context.read<FlowBloc>().add(const FlowLiveReconnectRequested());
    }
  }

  @override
  Widget build(BuildContext context) => widget.child;
}
