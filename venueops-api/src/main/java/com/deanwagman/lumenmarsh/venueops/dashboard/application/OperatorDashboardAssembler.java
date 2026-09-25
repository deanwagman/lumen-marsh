package com.deanwagman.lumenmarsh.venueops.dashboard.application;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.Attraction;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;
import com.deanwagman.lumenmarsh.venueops.dashboard.api.OperatorDashboardResponse;
import com.deanwagman.lumenmarsh.venueops.dashboard.api.OperatorDashboardResponse.DashboardActivityResponse;
import com.deanwagman.lumenmarsh.venueops.dashboard.api.OperatorDashboardResponse.DashboardAdvisoryResponse;
import com.deanwagman.lumenmarsh.venueops.dashboard.api.OperatorDashboardResponse.DashboardAttractionResponse;
import com.deanwagman.lumenmarsh.venueops.dashboard.api.OperatorDashboardResponse.DashboardAttentionItemResponse;
import com.deanwagman.lumenmarsh.venueops.dashboard.api.OperatorDashboardResponse.DashboardFreshnessResponse;
import com.deanwagman.lumenmarsh.venueops.dashboard.api.OperatorDashboardResponse.DashboardIncidentResponse;
import com.deanwagman.lumenmarsh.venueops.dashboard.api.OperatorDashboardResponse.DashboardSourceFreshnessResponse;
import com.deanwagman.lumenmarsh.venueops.dashboard.api.OperatorDashboardResponse.DashboardSummaryResponse;
import com.deanwagman.lumenmarsh.venueops.dashboard.api.OperatorDashboardResponse.DashboardWeatherResponse;
import com.deanwagman.lumenmarsh.venueops.dashboard.domain.DashboardAttentionKind;
import com.deanwagman.lumenmarsh.venueops.dashboard.domain.DashboardFreshnessStatus;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendation;
import com.deanwagman.lumenmarsh.venueops.flow.domain.FlowRecommendationStatus;
import com.deanwagman.lumenmarsh.venueops.incident.domain.Incident;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentStatus;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenancePriority;
import com.deanwagman.lumenmarsh.venueops.maintenance.domain.workorder.MaintenanceWorkOrder;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendation;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationActivity;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationEventType;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationOperatorStatus;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class OperatorDashboardAssembler {

    public static final int RECENT_ACTIVITY_LIMIT = 20;
    public static final Duration STALE_AFTER = Duration.ofMinutes(5);

    private OperatorDashboardAssembler() {
    }

    public static OperatorDashboardResponse assemble(
            Instant generatedAt,
            List<Attraction> attractions,
            List<Incident> incidents,
            List<WeatherRecommendation> recommendations
    ) {
        return assemble(generatedAt, attractions, incidents, recommendations, List.of(), List.of());
    }

    public static OperatorDashboardResponse assemble(
            Instant generatedAt,
            List<Attraction> attractions,
            List<Incident> incidents,
            List<WeatherRecommendation> recommendations,
            List<MaintenanceWorkOrder> workOrders,
            List<FlowRecommendation> flowRecommendations
    ) {
        List<Incident> openIncidents = incidents.stream()
                .filter(incident -> !incident.status().isResolved())
                .sorted(incidentOrder())
                .toList();
        List<Attraction> attentionAttractions = attractions.stream()
                .filter(OperatorDashboardAssembler::needsAttention)
                .sorted(attractionOrder())
                .toList();
        List<WeatherRecommendation> pendingWeather = recommendations.stream()
                .filter(OperatorDashboardAssembler::isPending)
                .sorted(weatherOrder())
                .toList();
        List<Incident> publishedAdvisories = incidents.stream()
                .filter(Incident::hasActiveGuestAdvisory)
                .sorted(Comparator.comparing(Incident::updatedAt).reversed().thenComparing(incident -> incident.id().value()))
                .toList();
        List<MaintenanceWorkOrder> openP1WorkOrders = workOrders.stream()
                .filter(OperatorDashboardAssembler::isOpenP1)
                .sorted(workOrderOrder())
                .toList();
        List<FlowRecommendation> unpublishedFlow = flowRecommendations.stream()
                .filter(OperatorDashboardAssembler::isUnpublished)
                .sorted(flowOrder())
                .toList();
        Map<String, Integer> relatedIncidents = relatedOpenIncidentCounts(openIncidents);

        DashboardSummaryResponse summary = new DashboardSummaryResponse(
                (int) attractions.stream().filter(attraction -> attraction.status() == AttractionStatus.OPERATING).count(),
                attentionAttractions.size(),
                (int) attractions.stream().filter(attraction -> attraction.status() == AttractionStatus.CLOSED).count(),
                (int) attractions.stream().filter(attraction -> attraction.status() == AttractionStatus.WEATHER_HOLD).count(),
                openIncidents.size(),
                (int) openIncidents.stream().filter(OperatorDashboardAssembler::majorOrCritical).count(),
                pendingWeather.size(),
                publishedAdvisories.size(),
                openP1WorkOrders.size(),
                unpublishedFlow.size()
        );

        DashboardFreshnessResponse freshness = freshness(generatedAt, attractions, incidents, recommendations);

        return new OperatorDashboardResponse(
                generatedAt,
                summary,
                needsAttention(
                        openIncidents,
                        attentionAttractions,
                        pendingWeather,
                        incidents,
                        freshness,
                        openP1WorkOrders,
                        unpublishedFlow
                ),
                openIncidents.stream().map(OperatorDashboardAssembler::incidentItem).toList(),
                attentionAttractions.stream()
                        .map(attraction -> attractionItem(attraction, relatedIncidents.getOrDefault(attraction.id().value(), 0)))
                        .toList(),
                pendingWeather.stream().map(OperatorDashboardAssembler::weatherItem).toList(),
                publishedAdvisories.stream().map(OperatorDashboardAssembler::advisoryItem).toList(),
                recentActivity(attractions, incidents, recommendations),
                freshness
        );
    }

    static boolean needsAttention(Attraction attraction) {
        if (attraction.status() == AttractionStatus.WEATHER_HOLD
                || attraction.status() == AttractionStatus.TECHNICAL_DELAY
                || attraction.status() == AttractionStatus.TESTING
                || attraction.status() == AttractionStatus.RETURNING_TO_SERVICE) {
            return true;
        }
        if (attraction.status() == AttractionStatus.OPERATING && attraction.capacityMode() == CapacityMode.REDUCED) {
            return true;
        }
        return attraction.status() == AttractionStatus.CLOSED && attraction.version() > 0;
    }

    private static boolean isPending(WeatherRecommendation recommendation) {
        return recommendation.operatorStatus() == WeatherRecommendationOperatorStatus.PENDING
                && recommendation.status() == WeatherRecommendationStatus.ACTIVE;
    }

    private static boolean isOpenP1(MaintenanceWorkOrder workOrder) {
        return workOrder.priority() == MaintenancePriority.P1 && workOrder.status().isActive();
    }

    private static boolean isUnpublished(FlowRecommendation recommendation) {
        return recommendation.status().isPending()
                || recommendation.status() == FlowRecommendationStatus.APPROVED;
    }

    private static boolean majorOrCritical(Incident incident) {
        return incident.severity() == IncidentSeverity.CRITICAL || incident.severity() == IncidentSeverity.MAJOR;
    }

    private static List<DashboardAttentionItemResponse> needsAttention(
            List<Incident> openIncidents,
            List<Attraction> attentionAttractions,
            List<WeatherRecommendation> pendingWeather,
            List<Incident> allIncidents,
            DashboardFreshnessResponse freshness,
            List<MaintenanceWorkOrder> openP1WorkOrders,
            List<FlowRecommendation> unpublishedFlow
    ) {
        List<DashboardAttentionItemResponse> items = new ArrayList<>();
        for (Incident incident : openIncidents) {
            if (incident.severity() == IncidentSeverity.CRITICAL) {
                items.add(attention(
                        DashboardAttentionKind.CRITICAL_INCIDENT,
                        "Critical incident is open and needs a supervisor-led response.",
                        "/incidents/" + incident.id().value(),
                        incident.id().value(),
                        incident.title(),
                        incident.updatedAt()
                ));
            } else if (incident.severity() == IncidentSeverity.MAJOR) {
                items.add(attention(
                        DashboardAttentionKind.MAJOR_INCIDENT,
                        "Major incident is open.",
                        "/incidents/" + incident.id().value(),
                        incident.id().value(),
                        incident.title(),
                        incident.updatedAt()
                ));
            }
        }
        for (WeatherRecommendation recommendation : pendingWeather) {
            if (recommendation.severity() == WeatherRecommendationSeverity.WARNING) {
                items.add(attention(
                        DashboardAttentionKind.WEATHER_HAZARD,
                        "Pending warning-level weather recommendation: " + recommendation.recommendedAction(),
                        "/attractions",
                        recommendation.id().value(),
                        recommendation.summary(),
                        recommendation.updatedAt()
                ));
            }
        }
        for (MaintenanceWorkOrder workOrder : openP1WorkOrders) {
            items.add(attention(
                    DashboardAttentionKind.OPEN_P1_WORK_ORDER,
                    "P1 work order " + workOrder.workOrderNumber() + " is open: " + workOrder.summary(),
                    "/maintenance/work-orders/" + workOrder.id(),
                    workOrder.id().toString(),
                    workOrder.workOrderNumber(),
                    workOrder.updatedAt()
            ));
        }
        for (Attraction attraction : attentionAttractions) {
            if (attraction.status() == AttractionStatus.WEATHER_HOLD) {
                items.add(attention(
                        DashboardAttentionKind.WEATHER_HOLD,
                        attraction.name() + " is on weather hold.",
                        "/attractions/" + attraction.id().value(),
                        attraction.id().value(),
                        attraction.name(),
                        attraction.updatedAt()
                ));
            } else {
                items.add(attention(
                        DashboardAttentionKind.ABNORMAL_ATTRACTION,
                        attractionReason(attraction),
                        "/attractions/" + attraction.id().value(),
                        attraction.id().value(),
                        attraction.name(),
                        attraction.updatedAt()
                ));
            }
        }
        for (FlowRecommendation recommendation : unpublishedFlow) {
            items.add(attention(
                    DashboardAttentionKind.UNPUBLISHED_FLOW_RECOMMENDATION,
                    "Unpublished flow recommendation: " + recommendation.summary(),
                    "/park-flow",
                    recommendation.id().value(),
                    recommendation.summary(),
                    recommendation.updatedAt()
            ));
        }
        for (Incident incident : openIncidents) {
            if (!majorOrCritical(incident) && incident.assignedTo() == null) {
                items.add(attention(
                        DashboardAttentionKind.UNASSIGNED_INCIDENT,
                        "Incident is open and unassigned.",
                        "/incidents/" + incident.id().value() + "?assignment=unassigned",
                        incident.id().value(),
                        incident.title(),
                        incident.updatedAt()
                ));
            }
            if (!majorOrCritical(incident) && incident.status() == IncidentStatus.REPORTED) {
                items.add(attention(
                        DashboardAttentionKind.UNACKNOWLEDGED_INCIDENT,
                        "Incident has not been acknowledged.",
                        "/incidents/" + incident.id().value(),
                        incident.id().value(),
                        incident.title(),
                        incident.updatedAt()
                ));
            }
        }
        for (Incident incident : allIncidents) {
            if (incident.status().isResolved() && incident.guestAdvisoryPublished()) {
                items.add(attention(
                        DashboardAttentionKind.ORPHAN_ADVISORY,
                        "Published guest advisory is associated with a resolved incident.",
                        "/incidents/" + incident.id().value(),
                        incident.id().value(),
                        incident.guestTitle() == null ? incident.title() : incident.guestTitle(),
                        incident.updatedAt()
                ));
            }
        }
        if (freshness.venueOps().status() == DashboardFreshnessStatus.STALE
                || freshness.environmentalData().status() == DashboardFreshnessStatus.STALE) {
            items.add(attention(
                    DashboardAttentionKind.STALE_DATA,
                    "Some operational information may be out of date.",
                    "/dashboard",
                    "freshness",
                    "Data freshness",
                    generatedAtOr(freshness)
            ));
        }
        items.sort(attentionOrder());
        return items;
    }

    private static Instant generatedAtOr(DashboardFreshnessResponse freshness) {
        Instant venueOps = freshness.venueOps().lastUpdatedAt();
        Instant environmental = freshness.environmentalData().lastUpdatedAt();
        if (venueOps == null) {
            return environmental;
        }
        if (environmental == null) {
            return venueOps;
        }
        return venueOps.isAfter(environmental) ? venueOps : environmental;
    }

    private static DashboardAttentionItemResponse attention(
            DashboardAttentionKind kind,
            String reason,
            String href,
            String subjectId,
            String subjectLabel,
            Instant updatedAt
    ) {
        return new DashboardAttentionItemResponse(kind, reason, href, subjectId, subjectLabel, updatedAt);
    }

    private static DashboardIncidentResponse incidentItem(Incident incident) {
        return new DashboardIncidentResponse(
                incident.id().value(),
                incident.title(),
                incident.severity(),
                incident.status(),
                incident.assignedTo(),
                incident.attractionIds().stream().map(id -> id.value()).toList(),
                incident.hasActiveGuestAdvisory(),
                incident.updatedAt(),
                incident.version()
        );
    }

    private static DashboardAttractionResponse attractionItem(Attraction attraction, int relatedOpenIncidentCount) {
        return new DashboardAttractionResponse(
                attraction.id().value(),
                attraction.name(),
                attraction.area(),
                attraction.status(),
                attraction.capacityMode(),
                attraction.waitMinutes(),
                attraction.guestStatusMessage(),
                attraction.updatedAt(),
                relatedOpenIncidentCount
        );
    }

    private static DashboardWeatherResponse weatherItem(WeatherRecommendation recommendation) {
        return new DashboardWeatherResponse(
                recommendation.id().value(),
                recommendation.severity(),
                recommendation.recommendedAction(),
                recommendation.evidence(),
                recommendation.affectedAttractionIds().stream().map(id -> id.value()).toList(),
                recommendation.operatorStatus(),
                recommendation.linkedIncidentId() == null ? null : recommendation.linkedIncidentId().value(),
                recommendation.observedAt(),
                recommendation.updatedAt(),
                recommendation.simulated()
        );
    }

    private static DashboardAdvisoryResponse advisoryItem(Incident incident) {
        return new DashboardAdvisoryResponse(
                incident.id().value(),
                incident.guestTitle(),
                incident.guestMessage(),
                incident.severity(),
                incident.attractionIds().stream().map(id -> id.value()).toList(),
                incident.updatedAt(),
                incident.id().value()
        );
    }

    private static List<DashboardActivityResponse> recentActivity(
            List<Attraction> attractions,
            List<Incident> incidents,
            List<WeatherRecommendation> recommendations
    ) {
        Map<String, String> attractionNames = new HashMap<>();
        for (Attraction attraction : attractions) {
            attractionNames.put(attraction.id().value(), attraction.name());
        }
        Stream<DashboardActivityResponse> attractionEvents = attractions.stream()
                .flatMap(attraction -> attraction.activity().stream().map(activity ->
                        new DashboardActivityResponse(
                                activity.occurredAt(),
                                activity.actor(),
                                "ATTRACTION",
                                activity.type().name(),
                                attraction.name(),
                                activity.reason(),
                                activity.resultingVersion(),
                                "/attractions/" + attraction.id().value()
                        )));
        Stream<DashboardActivityResponse> incidentEvents = incidents.stream()
                .flatMap(incident -> incident.activity().stream().map(activity ->
                        new DashboardActivityResponse(
                                activity.occurredAt(),
                                activity.actor(),
                                "INCIDENT",
                                activity.type().name(),
                                incident.title(),
                                activity.reason(),
                                activity.resultingVersion(),
                                "/incidents/" + incident.id().value()
                        )));
        Stream<DashboardActivityResponse> weatherEvents = recommendations.stream()
                .flatMap(recommendation -> recommendation.activity().stream()
                        .filter(OperatorDashboardAssembler::isWeatherReviewAction)
                        .map(activity -> new DashboardActivityResponse(
                                activity.occurredAt(),
                                activity.actor(),
                                "WEATHER",
                                activity.type().name(),
                                recommendation.summary(),
                                activity.reason(),
                                activity.resultingVersion(),
                                "/attractions"
                        )));
        return Stream.concat(attractionEvents, Stream.concat(incidentEvents, weatherEvents))
                .sorted(Comparator
                        .comparing(DashboardActivityResponse::occurredAt, Comparator.reverseOrder())
                        .thenComparing(DashboardActivityResponse::action)
                        .thenComparing(DashboardActivityResponse::subject))
                .limit(RECENT_ACTIVITY_LIMIT)
                .toList();
    }

    private static boolean isWeatherReviewAction(WeatherRecommendationActivity activity) {
        return activity.type() == WeatherRecommendationEventType.ACKNOWLEDGED
                || activity.type() == WeatherRecommendationEventType.DISMISSED
                || activity.type() == WeatherRecommendationEventType.INCIDENT_LINKED;
    }

    private static DashboardFreshnessResponse freshness(
            Instant generatedAt,
            List<Attraction> attractions,
            List<Incident> incidents,
            List<WeatherRecommendation> recommendations
    ) {
        Instant venueOpsUpdated = Stream.concat(
                        attractions.stream().map(Attraction::updatedAt),
                        Stream.concat(
                                incidents.stream().map(Incident::updatedAt),
                                recommendations.stream().map(WeatherRecommendation::updatedAt)
                        )
                )
                .max(Instant::compareTo)
                .orElse(null);
        Instant environmentalUpdated = recommendations.stream()
                .flatMap(recommendation -> Stream.of(recommendation.observedAt(), recommendation.receivedAt(), recommendation.updatedAt()))
                .max(Instant::compareTo)
                .orElse(null);
        return new DashboardFreshnessResponse(
                sourceFreshness(generatedAt, venueOpsUpdated),
                sourceFreshness(generatedAt, environmentalUpdated)
        );
    }

    static DashboardSourceFreshnessResponse sourceFreshness(Instant generatedAt, Instant lastUpdatedAt) {
        if (lastUpdatedAt == null) {
            return new DashboardSourceFreshnessResponse(DashboardFreshnessStatus.UNAVAILABLE, null);
        }
        DashboardFreshnessStatus status = lastUpdatedAt.plus(STALE_AFTER).isBefore(generatedAt)
                ? DashboardFreshnessStatus.STALE
                : DashboardFreshnessStatus.LIVE;
        return new DashboardSourceFreshnessResponse(status, lastUpdatedAt);
    }

    private static Map<String, Integer> relatedOpenIncidentCounts(List<Incident> openIncidents) {
        Map<String, Integer> counts = new HashMap<>();
        for (Incident incident : openIncidents) {
            for (var attractionId : incident.attractionIds()) {
                counts.merge(attractionId.value(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private static String attractionReason(Attraction attraction) {
        if (attraction.status() == AttractionStatus.OPERATING && attraction.capacityMode() == CapacityMode.REDUCED) {
            return attraction.name() + " is operating at reduced capacity.";
        }
        if (attraction.status() == AttractionStatus.CLOSED && attraction.version() > 0) {
            return attraction.name() + " closed unexpectedly.";
        }
        String message = attraction.guestStatusMessage();
        if (message != null && !message.isBlank()) {
            return attraction.name() + ": " + message;
        }
        return attraction.name() + " is not operating normally.";
    }

    private static Comparator<Incident> incidentOrder() {
        return Comparator
                .comparingInt((Incident incident) -> severityRank(incident.severity()))
                .thenComparing(Incident::updatedAt, Comparator.reverseOrder())
                .thenComparing(incident -> incident.id().value());
    }

    private static Comparator<Attraction> attractionOrder() {
        return Comparator
                .comparingInt((Attraction attraction) -> attractionRank(attraction))
                .thenComparing(Attraction::updatedAt, Comparator.reverseOrder())
                .thenComparing(Attraction::name);
    }

    private static Comparator<WeatherRecommendation> weatherOrder() {
        return Comparator
                .comparingInt((WeatherRecommendation recommendation) -> weatherRank(recommendation.severity()))
                .thenComparing(WeatherRecommendation::updatedAt, Comparator.reverseOrder())
                .thenComparing(recommendation -> recommendation.id().value());
    }

    private static Comparator<MaintenanceWorkOrder> workOrderOrder() {
        return Comparator
                .comparing(MaintenanceWorkOrder::updatedAt, Comparator.reverseOrder())
                .thenComparing(workOrder -> workOrder.id().toString());
    }

    private static Comparator<FlowRecommendation> flowOrder() {
        return Comparator
                .comparing(FlowRecommendation::updatedAt, Comparator.reverseOrder())
                .thenComparing(recommendation -> recommendation.id().value());
    }

    private static Comparator<DashboardAttentionItemResponse> attentionOrder() {
        return Comparator
                .comparingInt((DashboardAttentionItemResponse item) -> attentionRank(item.kind()))
                .thenComparing(DashboardAttentionItemResponse::updatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(DashboardAttentionItemResponse::subjectId);
    }

    private static int severityRank(IncidentSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 0;
            case MAJOR -> 1;
            case MODERATE -> 2;
            case MINOR -> 3;
        };
    }

    private static int weatherRank(WeatherRecommendationSeverity severity) {
        return switch (severity) {
            case WARNING -> 0;
            case WATCH -> 1;
            case INFO -> 2;
        };
    }

    private static int attractionRank(Attraction attraction) {
        return switch (attraction.status()) {
            case TECHNICAL_DELAY -> 0;
            case WEATHER_HOLD -> 1;
            case CLOSED -> attraction.version() > 0 ? 2 : 7;
            case TESTING -> 3;
            case RETURNING_TO_SERVICE -> 4;
            case OPERATING -> attraction.capacityMode() == CapacityMode.REDUCED ? 5 : 6;
        };
    }

    private static int attentionRank(DashboardAttentionKind kind) {
        return switch (kind) {
            case CRITICAL_INCIDENT -> 0;
            case MAJOR_INCIDENT -> 1;
            case WEATHER_HAZARD -> 2;
            case WEATHER_HOLD -> 3;
            case OPEN_P1_WORK_ORDER -> 4;
            case ABNORMAL_ATTRACTION -> 5;
            case UNPUBLISHED_FLOW_RECOMMENDATION -> 6;
            case UNASSIGNED_INCIDENT, UNACKNOWLEDGED_INCIDENT -> 7;
            case ORPHAN_ADVISORY -> 8;
            case STALE_DATA -> 9;
        };
    }
}
