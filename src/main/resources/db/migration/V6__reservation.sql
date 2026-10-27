CREATE TABLE appointment_reservations
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),

    branch_id     UUID         NOT NULL,
    service_id    UUID         NOT NULL,
    consultant_id UUID         NOT NULL,
    customer_id   UUID,

    start_time    TIMESTAMPTZ  NOT NULL,

    token_hash    VARCHAR(255) NOT NULL UNIQUE,

    status        VARCHAR(20)  NOT NULL DEFAULT 'HELD',

    expires_at    TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at  TIMESTAMPTZ,
    released_at   TIMESTAMPTZ,

    CONSTRAINT fk_reservation_branch
        FOREIGN KEY (branch_id)
            REFERENCES branches (id),

    CONSTRAINT fk_reservation_service
        FOREIGN KEY (service_id)
            REFERENCES services (id),

    CONSTRAINT fk_reservation_consultant
        FOREIGN KEY (consultant_id)
            REFERENCES consultants (id),

    CONSTRAINT fk_reservation_customer
        FOREIGN KEY (customer_id)
            REFERENCES customers (id),

    CONSTRAINT chk_reservation_status
        CHECK (status IN ('HELD', 'CONFIRMED', 'RELEASED', 'EXPIRED')),

    CONSTRAINT chk_reservation_expiry
        CHECK (expires_at > created_at),

    CONSTRAINT chk_reservation_confirmed_at
        CHECK (
            (status <> 'CONFIRMED' OR confirmed_at IS NOT NULL)
            ),

    CONSTRAINT chk_reservation_released_at
        CHECK (
            (status <> 'RELEASED' OR released_at IS NOT NULL)
            )
);

CREATE INDEX idx_reservations_consultant_start
    ON appointment_reservations (consultant_id, start_time);

CREATE INDEX idx_reservations_status_expiry
    ON appointment_reservations (status, expires_at);

CREATE INDEX idx_reservations_customer
    ON appointment_reservations (customer_id);

ALTER TABLE appointments
    ADD COLUMN reservation_id UUID;

ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_reservation
        FOREIGN KEY (reservation_id)
            REFERENCES appointment_reservations (id);

ALTER TABLE appointments
    ADD CONSTRAINT uq_appointments_reservation
        UNIQUE (reservation_id);