CREATE TABLE historical_operating_evidence (
    dataset_id VARCHAR(96) NOT NULL REFERENCES historical_dataset (id) ON DELETE CASCADE,
    evidence_id VARCHAR(96) NOT NULL,
    episode_id VARCHAR(96) NOT NULL,
    attraction_id VARCHAR(64) NOT NULL,
    park_date DATE NOT NULL,
    classification VARCHAR(64) NOT NULL,
    summary TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    segment_activity_ids JSONB NOT NULL,
    PRIMARY KEY (dataset_id, evidence_id),
    FOREIGN KEY (dataset_id, attraction_id)
        REFERENCES historical_attraction_snapshot (dataset_id, attraction_id) ON DELETE CASCADE,
    CONSTRAINT historical_evidence_segments_present CHECK (jsonb_typeof(segment_activity_ids) = 'array')
);

CREATE INDEX historical_operating_evidence_day
    ON historical_operating_evidence (dataset_id, attraction_id, park_date, occurred_at, evidence_id);
