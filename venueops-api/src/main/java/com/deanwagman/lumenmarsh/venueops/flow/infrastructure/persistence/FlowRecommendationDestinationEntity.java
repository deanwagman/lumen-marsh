package com.deanwagman.lumenmarsh.venueops.flow.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "flow_recommendation_destination")
public class FlowRecommendationDestinationEntity {

    @EmbeddedId
    private FlowRecommendationDestinationId id;

    protected FlowRecommendationDestinationEntity() {
    }

    public FlowRecommendationDestinationEntity(String recommendationId, String attractionId, String kind) {
        this.id = new FlowRecommendationDestinationId(recommendationId, attractionId, kind);
    }

    public FlowRecommendationDestinationId getId() {
        return id;
    }

    @Embeddable
    public static class FlowRecommendationDestinationId implements Serializable {
        @Column(name = "recommendation_id", length = 36)
        private String recommendationId;

        @Column(name = "attraction_id", length = 64)
        private String attractionId;

        @Column(length = 32)
        private String kind;

        protected FlowRecommendationDestinationId() {
        }

        public FlowRecommendationDestinationId(String recommendationId, String attractionId, String kind) {
            this.recommendationId = recommendationId;
            this.attractionId = attractionId;
            this.kind = kind;
        }

        public String getRecommendationId() {
            return recommendationId;
        }

        public String getAttractionId() {
            return attractionId;
        }

        public String getKind() {
            return kind;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof FlowRecommendationDestinationId that)) {
                return false;
            }
            return Objects.equals(recommendationId, that.recommendationId)
                    && Objects.equals(attractionId, that.attractionId)
                    && Objects.equals(kind, that.kind);
        }

        @Override
        public int hashCode() {
            return Objects.hash(recommendationId, attractionId, kind);
        }
    }
}
