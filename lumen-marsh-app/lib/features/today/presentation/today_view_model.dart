import 'package:equatable/equatable.dart';

import '../../favorites/presentation/saved_adventures_resolver.dart';
import '../../attractions/domain/attraction_enums.dart';
import '../../attractions/domain/attraction_summary.dart';

class TodayViewModel extends Equatable {
  const TodayViewModel({
    required this.operatingCount,
    required this.totalCount,
    required this.parkSummary,
    required this.activeNotices,
    required this.recommended,
    required this.shortestWaitLabel,
    required this.lastUpdatedAt,
    required this.savedAdventures,
  });

  factory TodayViewModel.from({
    required List<AttractionSummary> attractions,
    List<String> favoriteIds = const [],
  }) {
    if (attractions.isEmpty) {
      return TodayViewModel(
        operatingCount: 0,
        totalCount: 0,
        parkSummary: 'No attractions listed',
        activeNotices: const [],
        recommended: null,
        shortestWaitLabel: 'No operating attractions',
        lastUpdatedAt: null,
        savedAdventures: const [],
      );
    }

    final operating = attractions.where((a) => a.status.isOperating).toList();
    final notices =
        attractions
            .where(
              (a) =>
                  a.status == AttractionStatus.weatherHold ||
                  a.status == AttractionStatus.technicalDelay,
            )
            .toList()
          ..sort((left, right) => left.name.compareTo(right.name));

    final recommended = _recommended(operating);
    final savedAdventures = resolveSavedAdventures(
      favoriteIds: favoriteIds,
      catalog: attractions,
    );

    return TodayViewModel(
      operatingCount: operating.length,
      totalCount: attractions.length,
      parkSummary:
          '${operating.length} of ${attractions.length} attractions operating',
      activeNotices: notices,
      recommended: recommended,
      shortestWaitLabel:
          recommended?.waitTimeLabel ?? 'No operating attractions',
      lastUpdatedAt: attractions
          .map((a) => a.updatedAt)
          .reduce((left, right) => left.isAfter(right) ? left : right),
      savedAdventures: savedAdventures,
    );
  }

  static AttractionSummary? _recommended(List<AttractionSummary> operating) {
    if (operating.isEmpty) {
      return null;
    }

    final sorted = [...operating]
      ..sort((left, right) {
        final leftWait = left.waitMinutes;
        final rightWait = right.waitMinutes;
        if (leftWait == null && rightWait == null) {
          return left.name.compareTo(right.name);
        }
        if (leftWait == null) {
          return 1;
        }
        if (rightWait == null) {
          return -1;
        }
        final waitCompare = leftWait.compareTo(rightWait);
        if (waitCompare != 0) {
          return waitCompare;
        }
        return left.name.compareTo(right.name);
      });

    return sorted.first;
  }

  final int operatingCount;
  final int totalCount;
  final String parkSummary;
  final List<AttractionSummary> activeNotices;
  final AttractionSummary? recommended;
  final String shortestWaitLabel;
  final DateTime? lastUpdatedAt;
  final List<AttractionSummary> savedAdventures;

  @override
  List<Object?> get props => [
    operatingCount,
    totalCount,
    parkSummary,
    activeNotices,
    recommended,
    shortestWaitLabel,
    lastUpdatedAt,
    savedAdventures,
  ];
}
