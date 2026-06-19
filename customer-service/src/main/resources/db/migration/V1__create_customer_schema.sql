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
