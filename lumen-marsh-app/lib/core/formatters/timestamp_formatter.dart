import 'package:intl/intl.dart';

String formatUpdatedAt(DateTime timestamp) {
  return DateFormat.yMMMd().add_jm().format(timestamp.toLocal());
}
