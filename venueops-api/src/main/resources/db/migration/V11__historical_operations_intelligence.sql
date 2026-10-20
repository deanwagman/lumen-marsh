CREATE TABLE historical_dataset (
    id VARCHAR(96) PRIMARY KEY,
    dataset_version VARCHAR(32) NOT NULL,
    seed BIGINT NOT NULL,
    manifest_checksum VARCHAR(64) NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    state VARCHAR(24) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    CONSTRAINT historical_dataset_date_range CHECK (to_date >= from_date)
);

CREATE TABLE historical_operating_calendar (
    dataset_id VARCHAR(96) NOT NULL REFERENCES historical_dataset (id) ON DELETE CASCADE,
    park_date DATE NOT NULL,
    zone_id VARCHAR(64) NOT NULL,
    scheduled_open TIME NOT NULL,
    scheduled_close TIME NOT NULL,
    calendar_version INTEGER NOT NULL,
    PRIMARY KEY (dataset_id, park_date)
);

CREATE TABLE historical_attraction_plan (
    dataset_id VARCHAR(96) NOT NULL REFERENCES historical_dataset (id) ON DELETE CASCADE,
    park_date DATE NOT NULL,
    attraction_id VARCHAR(64) NOT NULL,
    expected_open TIME NOT NULL,
    expected_close TIME NOT NULL,
    planned_closure BOOLEAN NOT NULL,
    closure_reason TEXT,
    plan_version INTEGER NOT NULL,
    PRIMARY KEY (dataset_id, park_date, attraction_id),
    FOREIGN KEY (dataset_id, park_date)
        REFERENCES historical_operating_calendar (dataset_id, park_date) ON DELETE CASCADE,
    CONSTRAINT planned_closure_reason CHECK (NOT planned_closure OR closure_reason IS NOT NULL)
);

CREATE TABLE historical_attraction_snapshot (
    dataset_id VARCHAR(96) NOT NULL REFERENCES historical_dataset (id) ON DELETE CASCADE,
    attraction_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    area VARCHAR(255) NOT NULL,
    attraction_type VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    capacity_mode VARCHAR(64) NOT NULL,
    wait_minutes INTEGER,
    updated_at TIMESTAMPTZ NOT NULL,
    aggregate_version BIGINT NOT NULL,
    PRIMARY KEY (dataset_id, attraction_id)
);

CREATE TABLE historical_attraction_activity (
    dataset_id VARCHAR(96) NOT NULL REFERENCES historical_dataset (id) ON DELETE CASCADE,
    activity_id VARCHAR(36) NOT NULL,
    source_id VARCHAR(36) NOT NULL,
    command_id VARCHAR(36) NOT NULL,
    attraction_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor VARCHAR(255) NOT NULL,
    reason TEXT,
    occurred_at TIMESTAMPTZ NOT NULL,
    previous_version BIGINT NOT NULL,
    resulting_version BIGINT NOT NULL,
    previous_status VARCHAR(64),
    new_status VARCHAR(64),
    previous_capacity_mode VARCHAR(64),
    new_capacity_mode VARCHAR(64),
    previous_wait_minutes INTEGER,
    new_wait_minutes INTEGER,
    PRIMARY KEY (dataset_id, activity_id),
    FOREIGN KEY (dataset_id, attraction_id)
        REFERENCES historical_attraction_snapshot (dataset_id, attraction_id) ON DELETE CASCADE,
    UNIQUE (dataset_id, source_id)
);

CREATE INDEX historical_activity_timeline
    ON historical_attraction_activity (dataset_id, attraction_id, occurred_at, resulting_version);

CREATE TABLE historical_processed_command (
    dataset_id VARCHAR(96) NOT NULL REFERENCES historical_dataset (id) ON DELETE CASCADE,
    command_id VARCHAR(36) NOT NULL,
    attraction_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    resulting_version BIGINT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    result_json JSONB NOT NULL,
    PRIMARY KEY (dataset_id, command_id),
    FOREIGN KEY (dataset_id, attraction_id)
        REFERENCES historical_attraction_snapshot (dataset_id, attraction_id) ON DELETE CASCADE
);
