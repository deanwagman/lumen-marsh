import '../../attractions/domain/attraction_summary.dart';
import 'guest_wait.dart';
import 'park_zone.dart';

class BestNextRecommendation {
  const BestNextRecommendation({
    required this.wait,
    required this.score,
    required this.reason,
    required this.walkingMinutes,
    required this.published,
    this.attraction,
  });

  final GuestWait wait;
  final int score;
  final String reason;
  final int walkingMinutes;
  final bool published;
  final AttractionSummary? attraction;

  String get attractionId => wait.attractionId;
  String get displayName => wait.displayName;

  String walkingLabel(String originZoneId) {
    final origin = ParkZone.forId(originZoneId).name;
    return 'About $walkingMinutes minutes from $origin';
  }
}

class BestNextRanker {
  const BestNextRanker();

  static const waitWeight = 40;
  static const forecastWeight = 30;
  static const proximityWeight = 20;
  static const favoriteBonus = 15;
  static const availabilityBonus = 20;
  static const publishedBonus = 12;
  static const delayedPenalty = 8;
  static const stalePenalty = 25;

  List<BestNextRecommendation> rank({
    required List<GuestWait> waits,
    required String originZoneId,
    List<AttractionSummary> catalog = const [],
    Set<String> favoriteIds = const {},
    List<GuestGuidance> publishedGuidance = const [],
    List<String>? previousOrder,
  }) {
    final publishedIds = {
      for (final guidance in publishedGuidance)
        ...guidance.recommendedDestinationIds,
    };
    final byId = {for (final attraction in catalog) attraction.id: attraction};
    final scored = <BestNextRecommendation>[];

    for (final wait in waits) {
      if (!wait.availability.isOperating) {
        continue;
      }
      final walking = ParkZone.walkingMinutes(originZoneId, wait.originZoneId);
      final published = publishedIds.contains(wait.attractionId);
      scored.add(
        BestNextRecommendation(
          wait: wait,
          score: _score(
            wait: wait,
            walkingMinutes: walking,
            favorite: favoriteIds.contains(wait.attractionId),
            published: published,
          ),
          reason: _reason(wait, publishedGuidance),
          walkingMinutes: walking,
          published: published,
          attraction: byId[wait.attractionId],
        ),
      );
    }

    scored.sort((left, right) {
      final scoreCompare = right.score.compareTo(left.score);
      if (scoreCompare != 0) {
        return scoreCompare;
      }
      return left.displayName.compareTo(right.displayName);
    });

    if (previousOrder != null &&
        previousOrder.isNotEmpty &&
        scored.any((item) => item.wait.isStale)) {
      return _stableOrder(scored, previousOrder);
    }

    return scored;
  }

  int _score({
    required GuestWait wait,
    required int walkingMinutes,
    required bool favorite,
    required bool published,
  }) {
    final posted = wait.postedWaitMinutes ?? waitWeight;
    final waitScore = waitWeight - (posted > waitWeight ? waitWeight : posted);
    final forecastMinutes = wait.forecastMinutes ?? posted;
    final useForecast = wait.freshness == QueueFreshness.fresh;
    final forecastScore = useForecast
        ? forecastWeight -
              (forecastMinutes > forecastWeight
                  ? forecastWeight
                  : forecastMinutes)
        : 0;
    final proximityScore =
        proximityWeight -
        (walkingMinutes > proximityWeight ? proximityWeight : walkingMinutes);
    final freshnessPenalty = switch (wait.freshness) {
      QueueFreshness.stale => stalePenalty,
      QueueFreshness.delayed => delayedPenalty,
      QueueFreshness.fresh => 0,
    };
    return waitScore +
        forecastScore +
        proximityScore +
        (favorite ? favoriteBonus : 0) +
        availabilityBonus +
        (published ? publishedBonus : 0) -
        freshnessPenalty;
  }

  String _reason(GuestWait wait, List<GuestGuidance> guidance) {
    for (final item in guidance) {
      if (item.recommends(wait.attractionId) &&
          item.guestMessage != null &&
          item.guestMessage!.trim().isNotEmpty) {
        return item.guestMessage!.trim();
      }
    }
    final posted = wait.postedWaitMinutes;
    final waitPhrase = posted == null
        ? 'Wait is not posted'
        : 'Its current wait is $posted minutes';
    return '${wait.displayName} is a good next choice. $waitPhrase and is expected to ${wait.trend.expectedPhrase}.';
  }

  List<BestNextRecommendation> _stableOrder(
    List<BestNextRecommendation> scored,
    List<String> previousOrder,
  ) {
    final byId = {for (final item in scored) item.attractionId: item};
    final ordered = <BestNextRecommendation>[];
    for (final id in previousOrder) {
      final item = byId.remove(id);
      if (item != null) {
        ordered.add(item);
      }
    }
    ordered.addAll(byId.values);
    return ordered;
  }
}
