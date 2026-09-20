import 'package:flutter/widgets.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import 'bloc/flow_bloc.dart';

FlowBloc? maybeFlowBloc(BuildContext context) {
  try {
    return BlocProvider.of<FlowBloc>(context, listen: false);
  } catch (_) {
    return null;
  }
}
