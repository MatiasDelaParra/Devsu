package com.devsu.customer.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

public record CustomerEventPayload(
        String customerId,
        String name,
        String identification,
        Boolean status,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant occurredAt
) {
}
