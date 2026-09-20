CREATE TABLE attraction_processed_commands (
    command_id VARCHAR(36) PRIMARY KEY,
    attraction_id VARCHAR(64) NOT NULL REFERENCES attraction (id),
    event_type VARCHAR(64) NOT NULL,
    resulting_version BIGINT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    result_json JSONB NOT NULL
);

CREATE TABLE incident_processed_commands (
    command_id VARCHAR(36) PRIMARY KEY,
    incident_id VARCHAR(36) NOT NULL REFERENCES incident (id),
    event_type VARCHAR(64) NOT NULL,
    resulting_version BIGINT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    result_json JSONB NOT NULL
);
