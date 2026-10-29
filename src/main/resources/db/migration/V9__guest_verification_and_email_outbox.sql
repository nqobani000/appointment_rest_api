-- users module: one guest customer per (case-insensitive) email
CREATE UNIQUE INDEX uq_customers_guest_email
    ON customers (lower(email))
    WHERE customer_type = 'GUEST';

-- users module: pending email verifications; the customer is only created once the code is verified
CREATE TABLE guest_verifications
(
    id          UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    email       VARCHAR(320) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    surname     VARCHAR(100) NOT NULL,
    phone       VARCHAR(32)  NOT NULL,
    code_hash   VARCHAR(64)  NOT NULL,
    attempts    INT          NOT NULL DEFAULT 0,
    expires_at  TIMESTAMPTZ  NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_guest_verifications_active
    ON guest_verifications (email, created_at DESC)
    WHERE consumed_at IS NULL;

-- notifications module: transactional outbox drained by a scheduled poller
CREATE TABLE email_outbox
(
    id              UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    dedupe_key      VARCHAR(100) NOT NULL UNIQUE,
    recipient       VARCHAR(320) NOT NULL,
    template        VARCHAR(50)  NOT NULL,
    variables       JSONB,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts        INT          NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_error      VARCHAR(500),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    sent_at         TIMESTAMPTZ,

    CONSTRAINT chk_email_outbox_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE INDEX idx_email_outbox_due
    ON email_outbox (next_attempt_at)
    WHERE status = 'PENDING';
