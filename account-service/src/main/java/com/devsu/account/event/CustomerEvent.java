package com.devsu.account.event;

import java.time.Instant;

public record CustomerEvent(
        CustomerEventType type,
        String customerId,
        String name,
        String identification,
        Boolean status,
        Instant occurredAt
) {
}
