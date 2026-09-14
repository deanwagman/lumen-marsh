-- Verified audit identity columns. Existing actor values become LEGACY subjects/display names.

ALTER TABLE attraction_activity
    ADD COLUMN actor_subject VARCHAR(255),
    ADD COLUMN actor_display_name VARCHAR(255),
    ADD COLUMN actor_type VARCHAR(32),
    ADD COLUMN actor_issuer VARCHAR(512);

UPDATE attraction_activity
SET actor_subject = actor,
    actor_display_name = actor,
    actor_type = 'LEGACY',
    actor_issuer = 'venueops'
WHERE actor_subject IS NULL;

ALTER TABLE attraction_activity
    ALTER COLUMN actor_subject SET NOT NULL,
    ALTER COLUMN actor_display_name SET NOT NULL,
    ALTER COLUMN actor_type SET NOT NULL,
    ALTER COLUMN actor_issuer SET NOT NULL;

ALTER TABLE incident_activity
    ADD COLUMN actor_subject VARCHAR(255),
    ADD COLUMN actor_display_name VARCHAR(255),
    ADD COLUMN actor_type VARCHAR(32),
    ADD COLUMN actor_issuer VARCHAR(512);

UPDATE incident_activity
SET actor_subject = actor,
    actor_display_name = actor,
    actor_type = 'LEGACY',
    actor_issuer = 'venueops'
WHERE actor_subject IS NULL;

ALTER TABLE incident_activity
    ALTER COLUMN actor_subject SET NOT NULL,
    ALTER COLUMN actor_display_name SET NOT NULL,
    ALTER COLUMN actor_type SET NOT NULL,
    ALTER COLUMN actor_issuer SET NOT NULL;

ALTER TABLE weather_recommendation_activity
    ADD COLUMN actor_subject VARCHAR(255),
    ADD COLUMN actor_display_name VARCHAR(255),
    ADD COLUMN actor_type VARCHAR(32),
    ADD COLUMN actor_issuer VARCHAR(512);

UPDATE weather_recommendation_activity
SET actor_subject = actor,
    actor_display_name = actor,
    actor_type = 'LEGACY',
    actor_issuer = 'venueops'
WHERE actor_subject IS NULL;

ALTER TABLE weather_recommendation_activity
    ALTER COLUMN actor_subject SET NOT NULL,
    ALTER COLUMN actor_display_name SET NOT NULL,
    ALTER COLUMN actor_type SET NOT NULL,
    ALTER COLUMN actor_issuer SET NOT NULL;
