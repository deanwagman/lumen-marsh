CREATE TABLE weather_recommendation (
    id VARCHAR(36) PRIMARY KEY,
    rule_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    operator_status VARCHAR(32) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    summary TEXT NOT NULL,
    evidence TEXT NOT NULL,
    recommended_action TEXT NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    source_version BIGINT NOT NULL,
    version BIGINT NOT NULL,
    linked_incident_id VARCHAR(36)
);

CREATE TABLE weather_recommendation_attraction (
    recommendation_id VARCHAR(36) NOT NULL REFERENCES weather_recommendation (id),
    attraction_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (recommendation_id, attraction_id)
);

CREATE INDEX weather_recommendation_attraction_attraction_id_idx
    ON weather_recommendation_attraction (attraction_id);

CREATE TABLE weather_recommendation_activity (
    id VARCHAR(36) PRIMARY KEY,
    recommendation_id VARCHAR(36) NOT NULL REFERENCES weather_recommendation (id),
    event_type VARCHAR(64) NOT NULL,
    actor VARCHAR(255) NOT NULL,
    reason TEXT,
    occurred_at TIMESTAMPTZ NOT NULL,
    previous_version BIGINT NOT NULL,
    resulting_version BIGINT NOT NULL,
    payload JSONB NOT NULL
);

CREATE INDEX weather_recommendation_activity_recommendation_id_resulting_version_idx
    ON weather_recommendation_activity (recommendation_id, resulting_version);
