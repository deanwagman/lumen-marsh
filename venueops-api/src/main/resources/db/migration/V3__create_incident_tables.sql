CREATE TABLE incident (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    type VARCHAR(64) NOT NULL,
    severity VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    internal_description TEXT,
    assigned_to VARCHAR(255),
    guest_title VARCHAR(255),
    guest_message TEXT,
    guest_advisory_published BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE incident_attraction (
    incident_id VARCHAR(36) NOT NULL REFERENCES incident (id),
    attraction_id VARCHAR(64) NOT NULL REFERENCES attraction (id),
    PRIMARY KEY (incident_id, attraction_id)
);

CREATE INDEX incident_attraction_attraction_id_idx
    ON incident_attraction (attraction_id);

CREATE TABLE incident_activity (
    id VARCHAR(36) PRIMARY KEY,
    incident_id VARCHAR(36) NOT NULL REFERENCES incident (id),
    event_type VARCHAR(64) NOT NULL,
    actor VARCHAR(255) NOT NULL,
    reason TEXT,
    occurred_at TIMESTAMPTZ NOT NULL,
    previous_version BIGINT NOT NULL,
    resulting_version BIGINT NOT NULL,
    payload JSONB NOT NULL
);

CREATE INDEX incident_activity_incident_id_resulting_version_idx
    ON incident_activity (incident_id, resulting_version);
