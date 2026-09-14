package com.deanwagman.lumenmarsh.venueops.dashboard.api;

import com.deanwagman.lumenmarsh.venueops.attraction.domain.AttractionStatus;
import com.deanwagman.lumenmarsh.venueops.attraction.domain.CapacityMode;
import com.deanwagman.lumenmarsh.venueops.dashboard.domain.DashboardAttentionKind;
import com.deanwagman.lumenmarsh.venueops.dashboard.domain.DashboardFreshnessStatus;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentSeverity;
import com.deanwagman.lumenmarsh.venueops.incident.domain.IncidentStatus;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationOperatorStatus;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationSeverity;

import java.time.Instant;
import java.util.List;

public record OperatorDashboardResponse(
        Instant generatedAt,
        DashboardSummaryResponse summary,
        List<DashboardAttentionItemResponse> needsAttention,
        List<DashboardIncidentResponse> openIncidents,
        List<DashboardAttractionResponse> attractionsNeedingAttention,
        List<DashboardWeatherResponse> pendingWeatherRecommendations,
        List<DashboardAdvisoryResponse> publishedGuestAdvisories,
        List<DashboardActivityResponse> recentActivity,
        DashboardFreshnessResponse freshness
) {
    public record DashboardSummaryResponse(
            int operatingAttractions,
            int attractionsNeedingAttention,
            int closedAttractions,
            int weatherHoldAttractions,
            int openIncidents,
            int majorOrCriticalIncidents,
            int pendingWeatherRecommendations,
            int publishedGuestAdvisories
    ) {
    }

    public record DashboardAttentionItemResponse(
            DashboardAttentionKind kind,
            String reason,
            String href,
            String subjectId,
            String subjectLabel,
            Instant updatedAt
    ) {
    }

    public record DashboardIncidentResponse(
            String id,
            String title,
            IncidentSeverity severity,
            IncidentStatus status,
            String assignedTo,
            List<String> attractionIds,
            boolean guestAdvisoryPublished,
            Instant updatedAt,
            long version
    ) {
    }

    public record DashboardAttractionResponse(
            String id,
            String name,
            String area,
            AttractionStatus status,
            CapacityMode capacityMode,
            Integer waitMinutes,
            String statusMessage,
            Instant updatedAt,
            int relatedOpenIncidentCount
    ) {
    }

    public record DashboardWeatherResponse(
            String id,
            WeatherRecommendationSeverity severity,
            String recommendedAction,
            String evidence,
            List<String> affectedAttractionIds,
            WeatherRecommendationOperatorStatus operatorStatus,
            String linkedIncidentId,
            Instant observedAt,
            Instant updatedAt,
            boolean simulated
    ) {
    }

    public record DashboardAdvisoryResponse(
            String id,
            String title,
            String message,
            IncidentSeverity severity,
            List<String> affectedAttractionIds,
            Instant updatedAt,
            String incidentId
    ) {
    }

    public record DashboardActivityResponse(
            Instant occurredAt,
            String actor,
            String domain,
            String action,
            String subject,
            String reason,
            long resultingVersion,
            String href
    ) {
    }

    public record DashboardFreshnessResponse(
            DashboardSourceFreshnessResponse venueOps,
            DashboardSourceFreshnessResponse environmentalData
    ) {
    }

    public record DashboardSourceFreshnessResponse(
            DashboardFreshnessStatus status,
            Instant lastUpdatedAt
    ) {
    }
}
