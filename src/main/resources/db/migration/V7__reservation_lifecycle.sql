ALTER TABLE services
    ADD COLUMN duration_minutes INTEGER NOT NULL DEFAULT 30,
    ADD CONSTRAINT chk_services_duration CHECK (duration_minutes > 0);

ALTER TABLE appointment_reservations
    ADD COLUMN end_time TIMESTAMPTZ,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE appointment_reservations r
SET end_time = r.start_time + make_interval(mins => s.duration_minutes)
FROM services s
WHERE s.id = r.service_id;

ALTER TABLE appointment_reservations
    ALTER COLUMN end_time SET NOT NULL,
    ADD CONSTRAINT chk_reservation_time CHECK (end_time > start_time);

-- Expiry cannot be expressed in an exclusion constraint; stale HELD rows are
-- flipped to EXPIRED under the consultant lock before a new hold is inserted.
ALTER TABLE appointment_reservations
    ADD CONSTRAINT ex_reservations_consultant_no_overlap EXCLUDE USING gist (
        consultant_id WITH =,
        tstzrange(start_time, end_time, '[)') WITH &&
        ) WHERE (status = 'HELD');

CREATE INDEX idx_reservations_held_expiry
    ON appointment_reservations (expires_at) WHERE status = 'HELD';
