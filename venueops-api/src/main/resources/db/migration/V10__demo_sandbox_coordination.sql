CREATE TABLE demo_sandbox_lease (
    singleton_id SMALLINT PRIMARY KEY CHECK (singleton_id = 1),
    token_hash VARCHAR(64),
    issued_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    hard_expires_at TIMESTAMPTZ,
    last_activity_at TIMESTAMPTZ,
    state VARCHAR(16) NOT NULL,
    failure_detail VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

INSERT INTO demo_sandbox_lease (singleton_id, state)
VALUES (1, 'EXPIRED');

CREATE TABLE demo_reset_execution (
    reset_id VARCHAR(36) NOT NULL,
    component VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    completed_at TIMESTAMPTZ,
    PRIMARY KEY (reset_id, component)
);
