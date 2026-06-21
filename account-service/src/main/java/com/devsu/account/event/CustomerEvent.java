package com.devsu.account.event;

import java.time.Instant;
import java.util.UUID;

public record CustomerEvent(
        UUID eventId,
        CustomerEventType type,
        String customerId,
        String name,
        String identification,
        Boolean status,
        Instant occurredAt
) {
}
