CREATE TABLE attraction (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    area VARCHAR(255) NOT NULL,
    type VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    capacity_mode VARCHAR(64) NOT NULL,
    wait_minutes INTEGER,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE attraction_activity (
    id VARCHAR(36) PRIMARY KEY,
    attraction_id VARCHAR(64) NOT NULL REFERENCES attraction (id),
    event_type VARCHAR(64) NOT NULL,
    actor VARCHAR(255) NOT NULL,
    reason TEXT,
    occurred_at TIMESTAMPTZ NOT NULL,
    previous_version BIGINT NOT NULL,
    resulting_version BIGINT NOT NULL,
    payload JSONB NOT NULL
);

CREATE INDEX attraction_activity_attraction_id_resulting_version_idx
    ON attraction_activity (attraction_id, resulting_version);
