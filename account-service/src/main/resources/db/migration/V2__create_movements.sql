CREATE TABLE account.movements (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    movement_type VARCHAR(10) NOT NULL,
    value NUMERIC(19, 2) NOT NULL,
    balance NUMERIC(19, 2) NOT NULL,
    account_id UUID NOT NULL,
    CONSTRAINT fk_movements_account
        FOREIGN KEY (account_id) REFERENCES account.accounts (id),
    CONSTRAINT ck_movements_type CHECK (movement_type IN ('CREDIT', 'DEBIT')),
    CONSTRAINT ck_movements_non_zero_value CHECK (value <> 0),
    CONSTRAINT ck_movements_non_negative_balance CHECK (balance >= 0)
);

CREATE INDEX idx_movements_account_date
    ON account.movements (account_id, occurred_at DESC);
