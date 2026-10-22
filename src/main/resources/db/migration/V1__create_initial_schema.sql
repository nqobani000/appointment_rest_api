CREATE
EXTENSION IF NOT EXISTS pgcrypto;
CREATE
EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE provinces
(
    code VARCHAR(40) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,

    CONSTRAINT chk_provinces_code_upper
        CHECK (code = upper(code) AND code ~ '^[A-Z]+(_[A-Z]+)*$'
)
    );

INSERT INTO provinces (code, name)
VALUES ('EASTERN_CAPE', 'Eastern Cape'),
       ('FREE_STATE', 'Free State'),
       ('GAUTENG', 'Gauteng'),
       ('KWAZULU_NATAL', 'KwaZulu-Natal'),
       ('LIMPOPO', 'Limpopo'),
       ('MPUMALANGA', 'Mpumalanga'),
       ('NORTHERN_CAPE', 'Northern Cape'),
       ('NORTH_WEST', 'North West'),
       ('WESTERN_CAPE', 'Western Cape');

CREATE TABLE locations
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    street_no   VARCHAR(20)  NOT NULL,
    street_name VARCHAR(150) NOT NULL,
    suburb      VARCHAR(100) NOT NULL,
    city        VARCHAR(100) NOT NULL,
    province    VARCHAR(40)  NOT NULL
        REFERENCES provinces (code) ON DELETE RESTRICT,
    postal_code VARCHAR(16)  NOT NULL,
    country     VARCHAR(100) NOT NULL DEFAULT 'South Africa',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_locations_country_rsa
        CHECK (country = 'South Africa'),
    CONSTRAINT chk_locations_postal_code_rsa
        CHECK (postal_code ~ '^[0-9]{4}$'
)
    );

CREATE TABLE users
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    surname     VARCHAR(100) NOT NULL,
    email       VARCHAR(320) NOT NULL UNIQUE,
    phone       VARCHAR(32),
    location_id UUID REFERENCES locations (id) ON DELETE RESTRICT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE customers
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(100) NOT NULL,
    surname       VARCHAR(100) NOT NULL,
    email         VARCHAR(320) NOT NULL,
    phone         VARCHAR(32)  NOT NULL,
    customer_type VARCHAR(20)  NOT NULL,
    user_id       UUID UNIQUE REFERENCES users (id) ON DELETE RESTRICT,

    CONSTRAINT chk_customers_type
        CHECK (customer_type IN ('CLIENT', 'GUEST')),
    CONSTRAINT chk_customers_type_user
        CHECK (
            (customer_type = 'CLIENT' AND user_id IS NOT NULL)
                OR
            (customer_type = 'GUEST' AND user_id IS NULL)
            )
);

CREATE TABLE branches
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(150) NOT NULL,
    location_id UUID         NOT NULL REFERENCES locations (id) ON DELETE RESTRICT
);

CREATE TABLE branch_operating_hours
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id   UUID     NOT NULL REFERENCES branches (id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL,
    opens_at    TIME     NOT NULL,
    closes_at   TIME     NOT NULL,

    CONSTRAINT chk_branch_hours_day
        CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_branch_hours_time
        CHECK (closes_at > opens_at),
    CONSTRAINT uq_branch_hours_window
        UNIQUE (branch_id, day_of_week, opens_at, closes_at),
    CONSTRAINT  ex_branch_hours_no_overlap EXCLUDE USING gist (
            branch_id WITH =,
            day_of_week WITH =,
            (tsrange(DATE '2000-01-01' + opens_at,
                     DATE '2000-01-01' + closes_at, '[)')) WITH &&
        )
);

CREATE TABLE service_categories
(
    code       VARCHAR(40) PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    sort_order SMALLINT     NOT NULL UNIQUE,

    CONSTRAINT chk_service_categories_code_upper
        CHECK (code = upper(code) AND code ~ '^[A-Z]+(_[A-Z]+)*$'
)
    );

INSERT INTO service_categories (code, name, sort_order)
VALUES ('ACCOUNT', 'Account services', 1),
       ('CASH', 'Cash services', 2),
       ('CARDS', 'Cards', 3),
       ('PAYMENTS', 'Payments and transactions', 4),
       ('STATEMENTS', 'Statements and account information', 5),
       ('CREDIT', 'Credit services', 6),
       ('SAVINGS', 'Savings and investments', 7),
       ('DIGITAL', 'Digital banking support', 8),
       ('OTHER', 'Other services', 9);


CREATE TABLE services
(
    id                      UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    code                    VARCHAR(64)  NOT NULL UNIQUE,
    category                VARCHAR(40)  NOT NULL
        REFERENCES service_categories (code) ON DELETE RESTRICT,
    name                    VARCHAR(150) NOT NULL UNIQUE,
    description             TEXT,
    available_to_registered BOOLEAN      NOT NULL DEFAULT TRUE,
    available_to_guest      BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_services_code_upper
        CHECK (code = upper(code) AND code ~ '^[A-Z]+(_[A-Z]+)*$'
) ,
    CONSTRAINT chk_services_at_least_one_customer_type
        CHECK (available_to_registered OR available_to_guest)
);


INSERT INTO services
(code, category, name, description,
 available_to_registered, available_to_guest)
VALUES
    -- Account services
    ('ACCOUNT_OPENING', 'ACCOUNT', 'Account Opening',
     'Open a new bank account, including product selection, identity verification and initial deposit.',
     TRUE, TRUE),
    ('ACCOUNT_CLOSURE', 'ACCOUNT', 'Account Closure',
     'Close an existing bank account, settle any outstanding balances and receive written confirmation.',
     TRUE, FALSE),
    ('ACCOUNT_MAINTENANCE', 'ACCOUNT', 'Account Maintenance',
     'Update or correct existing account details such as account type, linked beneficiaries and preferences.',
     TRUE, FALSE),
    ('PERSONAL_DETAILS_UPDATE', 'ACCOUNT', 'Personal Details Update',
     'Change personal information on record, including name, contact numbers, email address and residential address.',
     TRUE, FALSE),
    ('FICA_VERIFICATION', 'ACCOUNT', 'FICA Verification',
     'Complete or renew FICA verification by submitting identity documents and proof of residence for compliance.',
     TRUE, TRUE),

    -- Cash services
    ('CASH_DEPOSIT', 'CASH', 'Cash Deposit',
     'Deposit cash into an existing bank account at the branch counter, with a receipt issued for the transaction.',
     TRUE, FALSE),
    ('CASH_WITHDRAWAL', 'CASH', 'Cash Withdrawal',
     'Withdraw cash from an existing bank account at the branch counter, subject to daily limits and identity checks.',
     TRUE, FALSE),
    ('CASH_FOR_LOAN_REPAYMENT', 'CASH', 'Cash for Loan Repayment',
     'Make a cash payment towards an existing loan or credit facility at the branch counter.',
     TRUE, FALSE),
    ('COIN_DEPOSIT', 'CASH', 'Coin Deposit',
     'Deposit coins into an existing bank account, with counting and a receipt issued by branch staff.',
     TRUE, FALSE),

    -- Card services
    ('DEBIT_CARD_COLLECTION', 'CARDS', 'Debit Card Collection',
     'Collect a newly issued or reissued debit card that is ready for pickup at the branch.',
     TRUE, FALSE),
    ('DEBIT_CARD_REPLACEMENT', 'CARDS', 'Debit Card Replacement',
     'Report a lost, stolen or damaged debit card and arrange for a replacement to be issued.',
     TRUE, FALSE),
    ('CREDIT_CARD_COLLECTION', 'CARDS', 'Credit Card Collection',
     'Collect a newly issued or reissued credit card that is ready for pickup at the branch.',
     TRUE, FALSE),
    ('CARD_PIN_RESET', 'CARDS', 'Card PIN Reset',
     'Reset or unblock the PIN on an existing debit or credit card after identity verification.',
     TRUE, FALSE),
    ('CARD_LIMIT_UPDATE', 'CARDS', 'Card Limit Update',
     'Adjust the daily or per-transaction limits on an existing debit or credit card.',
     TRUE, FALSE),
    ('CARD_BLOCKING', 'CARDS', 'Card Blocking',
     'Immediately block a lost or stolen card to prevent unauthorised transactions.',
     TRUE, FALSE),

    -- Payments and transactions
    ('EFT_PAYMENT', 'PAYMENTS', 'EFT Payment',
     'Set up, amend or cancel an electronic funds transfer from an existing bank account.',
     TRUE, FALSE),
    ('PAYSHAP_PAYMENT', 'PAYMENTS', 'PayShap Payment',
     'Send or receive a PayShap instant payment linked to an existing bank account or mobile number.',
     TRUE, FALSE),
    ('STOP_ORDER_MAINTENANCE', 'PAYMENTS', 'Stop Order Maintenance',
     'Create, amend or cancel a stop order instruction on an existing bank account.',
     TRUE, FALSE),
    ('DEBIT_ORDER_DISPUTE', 'PAYMENTS', 'Debit Order Dispute',
     'Dispute an unauthorised or incorrect debit order and request a reversal or investigation.',
     TRUE, FALSE),
    ('PAYMENT_REVERSAL', 'PAYMENTS', 'Payment Reversal',
     'Request the reversal of an incorrect or duplicate payment made from an existing account.',
     TRUE, FALSE),

    -- Statements and account information
    ('ACCOUNT_BALANCE_ENQUIRY', 'STATEMENTS', 'Account Balance Enquiry',
     'Obtain the current balance and available funds on an existing bank account.',
     TRUE, FALSE),
    ('BANK_STATEMENT_REQUEST', 'STATEMENTS', 'Bank Statement Request',
     'Request a printed or electronic bank statement for a chosen period on an existing account.',
     TRUE, FALSE),
    ('PROOF_OF_ACCOUNT', 'STATEMENTS', 'Proof of Account',
     'Obtain a formal letter confirming account ownership, often required for visas or contracts.',
     TRUE, FALSE),
    ('TRANSACTION_HISTORY_REQUEST', 'STATEMENTS', 'Transaction History Request',
     'Request a detailed transaction history for a chosen period on an existing account.',
     TRUE, FALSE),

    -- Credit services
    ('PERSONAL_LOAN_ENQUIRY', 'CREDIT', 'Personal Loan Enquiry',
     'Discuss personal loan options, interest rates and affordability before deciding whether to apply.',
     TRUE, TRUE),
    ('PERSONAL_LOAN_APPLICATION', 'CREDIT', 'Personal Loan Application',
     'Submit a formal application for a personal loan, including affordability assessment and supporting documents.',
     TRUE, FALSE),
    ('CREDIT_CARD_APPLICATION', 'CREDIT', 'Credit Card Application',
     'Apply for a new credit card, including credit assessment and limit determination.',
     TRUE, FALSE),
    ('ACCESS_FACILITY_ENQUIRY', 'CREDIT', 'Access Facility Enquiry',
     'Discuss an access facility or overdraft on an existing account, including limits and costs.',
     TRUE, FALSE),
    ('HOME_LOAN_ENQUIRY', 'CREDIT', 'Home Loan Enquiry',
     'Discuss home loan options, affordability and pre-qualification before submitting an application.',
     TRUE, TRUE),
    ('LOAN_REPAYMENT_ENQUIRY', 'CREDIT', 'Loan Repayment Enquiry',
     'Review the repayment schedule, outstanding balance or early settlement of an existing loan.',
     TRUE, FALSE),

    -- Savings and investments
    ('SAVINGS_ACCOUNT_OPENING', 'SAVINGS', 'Savings Account Opening',
     'Open a new savings account, including product selection and an initial deposit.',
     TRUE, TRUE),
    ('FIXED_TERM_DEPOSIT_ENQUIRY', 'SAVINGS', 'Fixed Term Deposit Enquiry',
     'Discuss fixed-term deposit options, interest rates and maturity terms before opening a deposit.',
     TRUE, TRUE),
    ('TAX_FREE_SAVINGS_ENQUIRY', 'SAVINGS', 'Tax Free Savings Enquiry',
     'Discuss tax-free savings account options, contribution limits and expected returns.',
     TRUE, TRUE),
    ('SAVINGS_WITHDRAWAL', 'SAVINGS', 'Savings Withdrawal',
     'Withdraw funds from an existing savings account, subject to product notice periods.',
     TRUE, FALSE),

    -- Digital banking support
    ('MOBILE_BANKING_SUPPORT', 'DIGITAL', 'Mobile Banking Support',
     'Get help with the mobile banking app, including registration, login and transaction issues.',
     TRUE, FALSE),
    ('ONLINE_BANKING_SUPPORT', 'DIGITAL', 'Online Banking Support',
     'Get help with online banking, including login, password resets and transaction issues.',
     TRUE, FALSE),
    ('APP_ACCESS_RECOVERY', 'DIGITAL', 'App Access Recovery',
     'Recover access to a locked or inaccessible banking app after identity verification.',
     TRUE, FALSE),
    ('DIGITAL_TRANSACTION_SUPPORT', 'DIGITAL', 'Digital Transaction Support',
     'Resolve issues with digital transactions such as failed payments or app errors.',
     TRUE, FALSE),

    -- Other services
    ('SMART_ID_APPLICATION', 'OTHER', 'Smart ID Application',
     'Apply for a smart ID card or passport through the branch-assisted application process.',
     TRUE, TRUE),
    ('SMART_ID_COLLECTION', 'OTHER', 'Smart ID Collection',
     'Collect a smart ID card or passport that is ready for pickup at the branch.',
     TRUE, TRUE),
    ('FOREIGN_EXCHANGE_ASSISTANCE', 'OTHER', 'Foreign Exchange Assistance',
     'Buy or sell foreign currency, or arrange international transfers for travel or business.',
     TRUE, FALSE),
    ('GENERAL_CUSTOMER_SUPPORT', 'OTHER', 'General Customer Support',
     'Discuss any general banking query, complaint or feedback with a customer service consultant.',
     TRUE, TRUE);

CREATE TABLE consultants
(
    id      UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    name    VARCHAR(100) NOT NULL,
    surname VARCHAR(100) NOT NULL,
    email   VARCHAR(320) NOT NULL UNIQUE,
    status  VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT chk_consultants_status
        CHECK (status IN ('ACTIVE', 'AWAY', 'DEACTIVATED'))
);

CREATE TABLE consultant_branches
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consultant_id UUID NOT NULL REFERENCES consultants (id) ON DELETE RESTRICT,
    branch_id     UUID NOT NULL REFERENCES branches (id) ON DELETE RESTRICT,

    CONSTRAINT uq_consultant_branches_pair UNIQUE (consultant_id, branch_id)
);

CREATE TABLE consultant_services
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consultant_id UUID NOT NULL REFERENCES consultants (id) ON DELETE RESTRICT,
    service_id    UUID NOT NULL REFERENCES services (id) ON DELETE RESTRICT,

    CONSTRAINT uq_consultant_services_pair UNIQUE (consultant_id, service_id)
);

CREATE TABLE branch_services
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id  UUID NOT NULL REFERENCES branches (id) ON DELETE RESTRICT,
    service_id UUID NOT NULL REFERENCES services (id) ON DELETE RESTRICT,

    CONSTRAINT uq_branch_services_pair UNIQUE (branch_id, service_id)
);


CREATE TABLE consultant_availability
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consultant_id UUID     NOT NULL,
    branch_id     UUID     NOT NULL,
    day_of_week   SMALLINT NOT NULL,
    start_time    TIME     NOT NULL,
    end_time      TIME     NOT NULL,

    CONSTRAINT fk_availability_consultant_branch
        FOREIGN KEY (consultant_id, branch_id)
            REFERENCES consultant_branches (consultant_id, branch_id)
            ON DELETE RESTRICT,
    CONSTRAINT chk_availability_day
        CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_availability_time
        CHECK (end_time > start_time),
    CONSTRAINT uq_availability_window
        UNIQUE (consultant_id, branch_id, day_of_week, start_time, end_time),
    CONSTRAINT    ex_consultant_availability_no_overlap EXCLUDE USING gist (
            consultant_id WITH =,
            branch_id WITH =,
            day_of_week WITH =,
            (tsrange(DATE '2000-01-01' + start_time,
                     DATE '2000-01-01' + end_time, '[)')) WITH &&
        )
);

CREATE TABLE appointments
(
    id            UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    reference_no  VARCHAR(40) NOT NULL UNIQUE,
    customer_id   UUID        NOT NULL REFERENCES customers (id) ON DELETE RESTRICT,
    consultant_id UUID        NOT NULL,
    branch_id     UUID        NOT NULL,
    service_id    UUID        NOT NULL,
    start_time    TIMESTAMPTZ NOT NULL,
    end_time      TIMESTAMPTZ NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',

    CONSTRAINT fk_appointments_consultant_branch
        FOREIGN KEY (consultant_id, branch_id)
            REFERENCES consultant_branches (consultant_id, branch_id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_appointments_branch_service
        FOREIGN KEY (branch_id, service_id)
            REFERENCES branch_services (branch_id, service_id)
            ON DELETE RESTRICT,
    CONSTRAINT fk_appointments_consultant_service
        FOREIGN KEY (consultant_id, service_id)
            REFERENCES consultant_services (consultant_id, service_id)
            ON DELETE RESTRICT,
    CONSTRAINT chk_appointments_time
        CHECK (end_time > start_time),
    CONSTRAINT chk_appointments_status
        CHECK (status IN ('CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW')),

    CONSTRAINT    ex_appointments_consultant_no_overlap EXCLUDE USING gist (
            consultant_id WITH =,
            tstzrange(start_time, end_time, '[)') WITH &&
        ) WHERE (status = 'CONFIRMED')
);

CREATE INDEX idx_users_location ON users (location_id);
CREATE INDEX idx_branches_location ON branches (location_id);
CREATE INDEX idx_locations_province ON locations (province);
CREATE INDEX idx_branch_operating_hours_branch_day
    ON branch_operating_hours (branch_id, day_of_week, opens_at);
CREATE INDEX idx_services_category
    ON services (category);
CREATE INDEX idx_services_eligibility
    ON services (available_to_registered, available_to_guest) WHERE is_active = TRUE;
CREATE INDEX idx_consultant_branches_branch
    ON consultant_branches (branch_id);
CREATE INDEX idx_consultant_services_service
    ON consultant_services (service_id);
CREATE INDEX idx_branch_services_service
    ON branch_services (service_id);
CREATE INDEX idx_consultant_availability_branch_day
    ON consultant_availability (branch_id, day_of_week, start_time);
CREATE INDEX idx_appointments_customer
    ON appointments (customer_id);
CREATE INDEX idx_appointments_branch_service_start
    ON appointments (branch_id, service_id, start_time);
CREATE INDEX idx_appointments_consultant_start
    ON appointments (consultant_id, start_time);