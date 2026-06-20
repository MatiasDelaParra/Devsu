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
    CONSTRAINT ck_accounts_account_type CHECK (account_type IN ('SAVINGS', 'CHECKING'))
);

CREATE INDEX idx_customer_snapshots_customer_id
    ON account.customer_snapshots (customer_id);

CREATE INDEX idx_accounts_customer_id
    ON account.accounts (customer_id);

CREATE INDEX idx_accounts_account_number
    ON account.accounts (account_number);
