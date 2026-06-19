package com.devsu.customer.event;

import com.devsu.customer.domain.Customer;
import com.devsu.customer.outbox.OutboxEvent;
import com.devsu.customer.outbox.OutboxEventStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerEventFactory {

    private static final String CUSTOMER_AGGREGATE_TYPE = "CUSTOMER";

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxEvent create(Customer customer, CustomerEventType eventType) {
        Instant occurredAt = clock.instant();
        CustomerEventPayload payload = new CustomerEventPayload(
                customer.getCustomerId(),
                customer.getName(),
                customer.getIdentification(),
                customer.getStatus(),
                occurredAt
        );

        return OutboxEvent.builder()
                .aggregateType(CUSTOMER_AGGREGATE_TYPE)
                .aggregateId(customer.getCustomerId())
                .eventType(eventType.name())
                .payload(serialize(payload))
                .status(OutboxEventStatus.PENDING)
                .createdAt(occurredAt)
                .retryCount(0)
                .build();
    }

    private String serialize(CustomerEventPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo crear el evento del cliente", exception);
        }
    }
}
