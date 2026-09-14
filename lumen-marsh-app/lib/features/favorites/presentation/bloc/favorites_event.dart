import 'package:equatable/equatable.dart';

sealed class FavoritesEvent extends Equatable {
  const FavoritesEvent();

  @override
  List<Object?> get props => [];
}

final class FavoritesHydrated extends FavoritesEvent {
  const FavoritesHydrated();
}

final class FavoriteToggled extends FavoritesEvent {
  const FavoriteToggled(this.attractionId);

  final String attractionId;

  @override
  List<Object?> get props => [attractionId];
}
