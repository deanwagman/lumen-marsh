CREATE TABLE maintenance_assets (
    id VARCHAR(36) PRIMARY KEY,
    asset_code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    asset_type VARCHAR(32) NOT NULL,
    attraction_id VARCHAR(64) NOT NULL REFERENCES attraction (id),
    parent_asset_id VARCHAR(36) REFERENCES maintenance_assets (id),
    criticality VARCHAR(32) NOT NULL,
    service_status VARCHAR(32) NOT NULL,
    manufacturer VARCHAR(255),
    model VARCHAR(255),
    installed_at TIMESTAMPTZ,
    version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX maintenance_assets_attraction_id_idx
    ON maintenance_assets (attraction_id);
CREATE INDEX maintenance_assets_parent_asset_id_idx
    ON maintenance_assets (parent_asset_id);
CREATE INDEX maintenance_assets_asset_type_idx
    ON maintenance_assets (asset_type);

CREATE TABLE maintenance_work_orders (
    id VARCHAR(36) PRIMARY KEY,
    work_order_number VARCHAR(32) NOT NULL UNIQUE,
    asset_id VARCHAR(36) NOT NULL REFERENCES maintenance_assets (id),
    attraction_id VARCHAR(64) NOT NULL REFERENCES attraction (id),
    incident_id VARCHAR(36),
    source_type VARCHAR(32) NOT NULL,
    source_reference_id VARCHAR(128),
    classification VARCHAR(32) NOT NULL,
    priority VARCHAR(8) NOT NULL,
    status VARCHAR(32) NOT NULL,
    summary VARCHAR(255) NOT NULL,
    description TEXT,
    assigned_team VARCHAR(128),
    assigned_actor_subject VARCHAR(255),
    estimated_restore_at TIMESTAMPTZ,
    opened_at TIMESTAMPTZ,
    work_started_at TIMESTAMPTZ,
    ready_for_testing_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    canceled_at TIMESTAMPTZ,
    version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX maintenance_work_orders_status_priority_idx
    ON maintenance_work_orders (status, priority);
CREATE INDEX maintenance_work_orders_attraction_id_status_idx
    ON maintenance_work_orders (attraction_id, status);
CREATE INDEX maintenance_work_orders_asset_id_status_idx
    ON maintenance_work_orders (asset_id, status);
CREATE INDEX maintenance_work_orders_incident_id_idx
    ON maintenance_work_orders (incident_id);
CREATE INDEX maintenance_work_orders_assigned_team_status_idx
    ON maintenance_work_orders (assigned_team, status);
CREATE INDEX maintenance_work_orders_updated_at_idx
    ON maintenance_work_orders (updated_at);

CREATE TABLE maintenance_checklist_items (
    id VARCHAR(36) PRIMARY KEY,
    work_order_id VARCHAR(36) NOT NULL REFERENCES maintenance_work_orders (id),
    sequence INTEGER NOT NULL,
    label VARCHAR(255) NOT NULL,
    instructions TEXT,
    required BOOLEAN NOT NULL,
    result VARCHAR(32) NOT NULL,
    notes TEXT,
    completed_by_subject VARCHAR(255),
    completed_by_display_name VARCHAR(255),
    completed_at TIMESTAMPTZ,
    version BIGINT NOT NULL,
    UNIQUE (work_order_id, sequence)
);

CREATE TABLE maintenance_work_order_evidence (
    id VARCHAR(36) PRIMARY KEY,
    work_order_id VARCHAR(36) NOT NULL REFERENCES maintenance_work_orders (id),
    label VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    uri VARCHAR(1024) NOT NULL,
    added_by_subject VARCHAR(255) NOT NULL,
    added_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE maintenance_activity (
    id VARCHAR(36) PRIMARY KEY,
    work_order_id VARCHAR(36) NOT NULL REFERENCES maintenance_work_orders (id),
    sequence BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32),
    actor_subject VARCHAR(255) NOT NULL,
    actor_display_name VARCHAR(255) NOT NULL,
    reason TEXT,
    details JSONB NOT NULL,
    command_id VARCHAR(36) NOT NULL,
    correlation_id VARCHAR(128) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    resulting_version BIGINT NOT NULL,
    UNIQUE (work_order_id, sequence)
);

CREATE INDEX maintenance_activity_work_order_id_sequence_idx
    ON maintenance_activity (work_order_id, sequence);

CREATE TABLE maintenance_recommendations (
    id VARCHAR(36) PRIMARY KEY,
    observation_id VARCHAR(128) NOT NULL UNIQUE,
    observed_at TIMESTAMPTZ NOT NULL,
    asset_code VARCHAR(64) NOT NULL,
    asset_id VARCHAR(36) NOT NULL REFERENCES maintenance_assets (id),
    signal_type VARCHAR(32) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    value DOUBLE PRECISION,
    unit VARCHAR(32),
    evidence TEXT NOT NULL,
    recommended_action TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    work_order_id VARCHAR(36),
    command_id VARCHAR(36),
    received_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL
);

CREATE UNIQUE INDEX maintenance_recommendations_command_id_idx
    ON maintenance_recommendations (command_id)
    WHERE command_id IS NOT NULL;

CREATE INDEX maintenance_recommendations_status_observed_at_idx
    ON maintenance_recommendations (status, observed_at);

CREATE TABLE maintenance_processed_commands (
    command_id VARCHAR(36) PRIMARY KEY,
    work_order_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    resulting_version BIGINT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    result_json JSONB NOT NULL
);

CREATE TABLE maintenance_work_order_sequence (
    year INTEGER PRIMARY KEY,
    last_value BIGINT NOT NULL
);
