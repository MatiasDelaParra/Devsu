CREATE TABLE account.processed_customer_events (
    event_id UUID PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_processed_customer_events_customer_id
    ON account.processed_customer_events (customer_id);
