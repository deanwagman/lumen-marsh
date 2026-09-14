import 'package:flutter_bloc/flutter_bloc.dart';

import '../../domain/favorites_repository.dart';
import 'favorites_event.dart';
import 'favorites_state.dart';

class FavoritesBloc extends Bloc<FavoritesEvent, FavoritesState> {
  FavoritesBloc({required this.repository}) : super(const FavoritesInitial()) {
    on<FavoritesHydrated>(_onHydrated);
    on<FavoriteToggled>(_onToggled);
  }

  final FavoritesRepository repository;

  Future<void> _onHydrated(
    FavoritesHydrated event,
    Emitter<FavoritesState> emit,
  ) async {
    final ids = await repository.loadFavoriteIds();
    if (!isClosed) {
      emit(FavoritesLoaded(ids));
    }
  }

  Future<void> _onToggled(
    FavoriteToggled event,
    Emitter<FavoritesState> emit,
  ) async {
    final current = await _currentFavoriteIds();
    if (current.contains(event.attractionId)) {
      current.remove(event.attractionId);
    } else {
      current.add(event.attractionId);
    }

    await repository.saveFavoriteIds(current);
    emit(FavoritesLoaded(current));
  }

  Future<List<String>> _currentFavoriteIds() async {
    return switch (state) {
      FavoritesLoaded(:final favoriteIds) => [...favoriteIds],
      FavoritesInitial() => [...await repository.loadFavoriteIds()],
    };
  }
}
