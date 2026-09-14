CREATE TABLE attraction_experience (
    attraction_id VARCHAR(64) PRIMARY KEY REFERENCES attraction (id),
    short_description TEXT NOT NULL,
    duration_minutes INTEGER NOT NULL,
    minimum_height_inches INTEGER,
    intensity VARCHAR(64) NOT NULL,
    environment VARCHAR(64) NOT NULL,
    single_rider_available BOOLEAN NOT NULL,
    accessibility_summary TEXT NOT NULL,
    hero_url VARCHAR(512) NOT NULL,
    thumbnail_url VARCHAR(512) NOT NULL,
    alt_text TEXT NOT NULL
);
