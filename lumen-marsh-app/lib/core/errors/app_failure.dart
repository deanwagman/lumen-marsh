import 'package:equatable/equatable.dart';

sealed class AppFailure extends Equatable {
  const AppFailure(this.message);

  final String message;

  @override
  List<Object?> get props => [message];
}

final class NetworkFailure extends AppFailure {
  const NetworkFailure([
    super.message = "Can't reach VenueOps. Check your connection.",
  ]);
}

final class ServerFailure extends AppFailure {
  const ServerFailure([
    super.message = "VenueOps couldn't complete that request.",
  ]);
}

final class NotFoundFailure extends AppFailure {
  const NotFoundFailure([
    super.message = 'That attraction could not be found.',
  ]);
}

final class UnexpectedFailure extends AppFailure {
  const UnexpectedFailure([
    super.message = 'Something unexpected happened while talking to VenueOps.',
  ]);
}
