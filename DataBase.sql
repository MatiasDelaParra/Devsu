
-- ============================================================
-- customer-service / customer_db
-- ============================================================

CREATE SCHEMA IF NOT EXISTS customer;

CREATE TABLE customer.persons (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    gender VARCHAR(20) NOT NULL,
    age INTEGER NOT NULL,
    identification VARCHAR(50) NOT NULL,
    address VARCHAR(200) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    CONSTRAINT ck_persons_age_non_negative CHECK (age >= 0),
    CONSTRAINT uk_persons_identification UNIQUE (identification)
);

CREATE TABLE customer.customers (
    person_id UUID PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    status BOOLEAN NOT NULL,
    CONSTRAINT fk_customers_persons
        FOREIGN KEY (person_id)
        REFERENCES customer.persons (id)
        ON DELETE CASCADE,
    CONSTRAINT uk_customers_customer_id UNIQUE (customer_id)
);

CREATE TABLE customer.outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    CONSTRAINT ck_outbox_events_status
        CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_outbox_events_retry_count_non_negative
        CHECK (retry_count >= 0)
);

CREATE INDEX ix_outbox_events_pending_created_at
    ON customer.outbox_events (status, created_at);

-- ============================================================
-- account-service / account_db
-- ============================================================

CREATE SCHEMA IF NOT EXISTS account;

CREATE TABLE account.customer_snapshots (
    id UUID PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    identification VARCHAR(50) NOT NULL,
    status BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_customer_snapshots_customer_id UNIQUE (customer_id)
);

CREATE TABLE account.accounts (
    id UUID PRIMARY KEY,
    account_number VARCHAR(50) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    initial_balance NUMERIC(19, 2) NOT NULL,
    current_balance NUMERIC(19, 2) NOT NULL,
    status BOOLEAN NOT NULL,
    customer_id VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_accounts_account_number UNIQUE (account_number),
    CONSTRAINT ck_accounts_account_type
        CHECK (account_type IN ('SAVINGS', 'CHECKING'))
);

CREATE TABLE account.movements (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    movement_type VARCHAR(10) NOT NULL,
    value NUMERIC(19, 2) NOT NULL,
    balance NUMERIC(19, 2) NOT NULL,
    account_id UUID NOT NULL,
    reversal_of_id UUID,
    reversal_reason VARCHAR(200),
    CONSTRAINT fk_movements_account
        FOREIGN KEY (account_id) REFERENCES account.accounts (id),
    CONSTRAINT fk_movements_reversal_of
        FOREIGN KEY (reversal_of_id) REFERENCES account.movements (id),
    CONSTRAINT uk_movements_reversal_of UNIQUE (reversal_of_id),
    CONSTRAINT ck_movements_type CHECK (movement_type IN ('CREDIT', 'DEBIT')),
    CONSTRAINT ck_movements_non_zero_value CHECK (value <> 0),
    CONSTRAINT ck_movements_non_negative_balance CHECK (balance >= 0),
    CONSTRAINT ck_movements_reversal_data
        CHECK (
            (reversal_of_id IS NULL AND reversal_reason IS NULL)
            OR
            (reversal_of_id IS NOT NULL AND reversal_reason IS NOT NULL)
        )
);

CREATE TABLE account.processed_customer_events (
    event_id UUID PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_customer_snapshots_customer_id
    ON account.customer_snapshots (customer_id);

CREATE INDEX idx_accounts_customer_id
    ON account.accounts (customer_id);

CREATE INDEX idx_accounts_account_number
    ON account.accounts (account_number);

CREATE INDEX idx_movements_account_date
    ON account.movements (account_id, occurred_at DESC);

CREATE INDEX idx_processed_customer_events_customer_id
    ON account.processed_customer_events (customer_id);
