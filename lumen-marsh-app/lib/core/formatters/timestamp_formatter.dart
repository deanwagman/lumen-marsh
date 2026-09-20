import 'package:intl/intl.dart';

String formatUpdatedAt(DateTime timestamp) {
  return DateFormat.yMMMd().add_jm().format(timestamp.toLocal());
}

String formatWaitAge(DateTime timestamp, {DateTime? now}) {
  final current = (now ?? DateTime.now()).toUtc();
  final age = current.difference(timestamp.toUtc());
  if (age.isNegative || age.inSeconds < 45) {
    return 'just now';
  }
  if (age.inMinutes < 60) {
    final minutes = age.inMinutes == 0 ? 1 : age.inMinutes;
    return '$minutes minute${minutes == 1 ? '' : 's'} ago';
  }
  return formatUpdatedAt(timestamp);
}
