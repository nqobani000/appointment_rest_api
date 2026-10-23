CREATE EXTENSION IF NOT EXISTS postgis;

ALTER TABLE locations
    ADD COLUMN coordinates GEOGRAPHY(POINT, 4326);

CREATE INDEX idx_locations_coordinates
    ON locations
        USING GIST (coordinates);