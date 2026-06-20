ALTER TABLE account.movements
    ADD COLUMN reversal_of_id UUID,
    ADD COLUMN reversal_reason VARCHAR(200);

ALTER TABLE account.movements
    ADD CONSTRAINT fk_movements_reversal_of
        FOREIGN KEY (reversal_of_id) REFERENCES account.movements (id),
    ADD CONSTRAINT uk_movements_reversal_of UNIQUE (reversal_of_id),
    ADD CONSTRAINT ck_movements_reversal_data
        CHECK (
            (reversal_of_id IS NULL AND reversal_reason IS NULL)
            OR
            (reversal_of_id IS NOT NULL AND reversal_reason IS NOT NULL)
        );
