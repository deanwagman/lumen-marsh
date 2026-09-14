package com.deanwagman.lumenmarsh.venueops.weather.infrastructure.persistence;

import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationOperatorStatus;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationSeverity;
import com.deanwagman.lumenmarsh.venueops.weather.domain.WeatherRecommendationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface WeatherRecommendationJpaRepository extends JpaRepository<WeatherRecommendationEntity, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update WeatherRecommendationEntity r
               set r.ruleId = :ruleId,
                   r.status = :status,
                   r.operatorStatus = :operatorStatus,
                   r.severity = :severity,
                   r.summary = :summary,
                   r.evidence = :evidence,
                   r.recommendedAction = :recommendedAction,
                   r.observedAt = :observedAt,
                   r.updatedAt = :updatedAt,
                   r.sourceVersion = :sourceVersion,
                   r.version = :newVersion,
                   r.linkedIncidentId = :linkedIncidentId
             where r.id = :id
               and r.version = :expectedVersion
            """)
    int updateOperationalState(
            @Param("id") String id,
            @Param("ruleId") String ruleId,
            @Param("status") WeatherRecommendationStatus status,
            @Param("operatorStatus") WeatherRecommendationOperatorStatus operatorStatus,
            @Param("severity") WeatherRecommendationSeverity severity,
            @Param("summary") String summary,
            @Param("evidence") String evidence,
            @Param("recommendedAction") String recommendedAction,
            @Param("observedAt") Instant observedAt,
            @Param("updatedAt") Instant updatedAt,
            @Param("sourceVersion") long sourceVersion,
            @Param("newVersion") long newVersion,
            @Param("linkedIncidentId") String linkedIncidentId,
            @Param("expectedVersion") long expectedVersion
    );
}
