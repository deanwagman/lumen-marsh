import 'package:equatable/equatable.dart';

sealed class FavoritesState extends Equatable {
  const FavoritesState();

  @override
  List<Object?> get props => [];
}

final class FavoritesInitial extends FavoritesState {
  const FavoritesInitial();
}

final class FavoritesLoaded extends FavoritesState {
  const FavoritesLoaded(this.favoriteIds);

  final List<String> favoriteIds;

  bool isFavorite(String attractionId) => favoriteIds.contains(attractionId);

  @override
  List<Object?> get props => [favoriteIds];
}
