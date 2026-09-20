CREATE TABLE flow_observation (
    observation_id VARCHAR(36) PRIMARY KEY,
    attraction_id VARCHAR(64) NOT NULL REFERENCES attraction (id),
    observed_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    window_seconds INTEGER NOT NULL,
    queue_length INTEGER NOT NULL,
    arrivals INTEGER NOT NULL,
    boarded INTEGER NOT NULL,
    operating_units INTEGER NOT NULL,
    configured_units INTEGER NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    simulated BOOLEAN NOT NULL
);

CREATE INDEX flow_observation_attraction_observed_at_idx
    ON flow_observation (attraction_id, observed_at DESC);

CREATE TABLE queue_projection (
    attraction_id VARCHAR(64) PRIMARY KEY REFERENCES attraction (id),
    observation_id VARCHAR(36) NOT NULL REFERENCES flow_observation (observation_id),
    queue_length INTEGER NOT NULL,
    arrivals_per_minute DOUBLE PRECISION NOT NULL,
    throughput_per_minute DOUBLE PRECISION NOT NULL,
    calculated_wait_minutes INTEGER NOT NULL,
    posted_wait_minutes INTEGER NOT NULL,
    operating_capacity_percent INTEGER NOT NULL,
    trend VARCHAR(16) NOT NULL,
    observed_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,
    simulated BOOLEAN NOT NULL
);

CREATE TABLE queue_forecast (
    forecast_id VARCHAR(36) PRIMARY KEY,
    attraction_id VARCHAR(64) NOT NULL REFERENCES attraction (id),
    generated_at TIMESTAMPTZ NOT NULL,
    based_on_observation_id VARCHAR(36) NOT NULL REFERENCES flow_observation (observation_id),
    horizon_minutes INTEGER NOT NULL,
    predicted_queue_length INTEGER NOT NULL,
    predicted_wait_minutes INTEGER NOT NULL,
    confidence VARCHAR(16) NOT NULL,
    assumptions JSONB NOT NULL,
    explanation TEXT NOT NULL,
    simulated BOOLEAN NOT NULL,
    UNIQUE (attraction_id, based_on_observation_id, horizon_minutes)
);

CREATE INDEX queue_forecast_attraction_generated_at_idx
    ON queue_forecast (attraction_id, generated_at DESC);

CREATE TABLE flow_recommendation (
    recommendation_id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    source_attraction_id VARCHAR(64) REFERENCES attraction (id),
    summary VARCHAR(512) NOT NULL,
    explanation TEXT NOT NULL,
    guest_message TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    related_incident_id VARCHAR(36),
    related_work_order_id VARCHAR(36),
    version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    simulated BOOLEAN NOT NULL
);

CREATE INDEX flow_recommendation_status_updated_at_idx
    ON flow_recommendation (status, updated_at DESC);

CREATE INDEX flow_recommendation_expires_at_idx
    ON flow_recommendation (expires_at);

CREATE TABLE flow_recommendation_destination (
    recommendation_id VARCHAR(36) NOT NULL REFERENCES flow_recommendation (recommendation_id),
    attraction_id VARCHAR(64) NOT NULL REFERENCES attraction (id),
    kind VARCHAR(32) NOT NULL,
    PRIMARY KEY (recommendation_id, attraction_id, kind)
);

CREATE TABLE flow_activity (
    id VARCHAR(36) PRIMARY KEY,
    recommendation_id VARCHAR(36) NOT NULL REFERENCES flow_recommendation (recommendation_id),
    sequence BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    actor_subject VARCHAR(255) NOT NULL,
    actor_display_name VARCHAR(255) NOT NULL,
    actor_type VARCHAR(32) NOT NULL,
    reason TEXT,
    details JSONB NOT NULL,
    command_id VARCHAR(36) NOT NULL,
    correlation_id VARCHAR(128) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    resulting_version BIGINT NOT NULL,
    UNIQUE (recommendation_id, sequence)
);

CREATE INDEX flow_activity_recommendation_id_sequence_idx
    ON flow_activity (recommendation_id, sequence);

CREATE TABLE flow_processed_commands (
    command_id VARCHAR(36) PRIMARY KEY,
    recommendation_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    resulting_version BIGINT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    result_json JSONB NOT NULL
);
