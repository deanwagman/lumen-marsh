import 'package:flutter/material.dart';

import '../../../../design_system/components/status/lumen_notice_banner.dart';
import '../../../../design_system/components/status/lumen_tone.dart';
import '../../domain/attraction_live.dart';
import '../bloc/attractions_state.dart';

class LiveConnectionBanner extends StatelessWidget {
  const LiveConnectionBanner({super.key, required this.state});

  final AttractionsState state;

  @override
  Widget build(BuildContext context) {
    final message = switch (state) {
      AttractionsLoaded(:final connectionStatus, :final connectionMessage) =>
        _messageFor(connectionStatus, connectionMessage),
      AttractionsRefreshing(
        :final connectionStatus,
        :final connectionMessage,
      ) =>
        _messageFor(connectionStatus, connectionMessage),
      _ => null,
    };

    if (message == null) {
      return const SizedBox.shrink();
    }

    final tone = switch (state) {
      AttractionsLoaded(:final connectionStatus) =>
        connectionStatus == LiveConnectionStatus.stale
            ? LumenTone.warning
            : LumenTone.informational,
      AttractionsRefreshing(:final connectionStatus) =>
        connectionStatus == LiveConnectionStatus.stale
            ? LumenTone.warning
            : LumenTone.informational,
      _ => LumenTone.informational,
    };

    return LumenNoticeBanner(tone: tone, message: message);
  }

  String? _messageFor(LiveConnectionStatus status, String? explicit) {
    if (explicit != null && explicit.isNotEmpty) {
      return explicit;
    }
    return switch (status) {
      LiveConnectionStatus.reconnecting =>
        'Reconnecting to live park conditions…',
      LiveConnectionStatus.stale =>
        'Live updates paused—showing last known conditions',
      LiveConnectionStatus.connecting || LiveConnectionStatus.connected => null,
    };
  }
}
