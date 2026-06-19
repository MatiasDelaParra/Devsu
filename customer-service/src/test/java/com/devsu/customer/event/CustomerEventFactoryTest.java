package com.devsu.customer.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.devsu.customer.domain.Customer;
import com.devsu.customer.outbox.OutboxEvent;
import com.devsu.customer.outbox.OutboxEventStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class CustomerEventFactoryTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-06-18T10:00:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final CustomerEventFactory eventFactory = new CustomerEventFactory(
            objectMapper,
            Clock.fixed(OCCURRED_AT, ZoneOffset.UTC)
    );

    @Test
    void createsPendingOutboxEventWithCustomerPayload() throws Exception {
        OutboxEvent event = eventFactory.create(customer(), CustomerEventType.CUSTOMER_CREATED);
        JsonNode payload = objectMapper.readTree(event.getPayload());

        assertThat(event.getAggregateType()).isEqualTo("CUSTOMER");
        assertThat(event.getAggregateId()).isEqualTo("CUS-001");
        assertThat(event.getEventType()).isEqualTo("CUSTOMER_CREATED");
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getCreatedAt()).isEqualTo(OCCURRED_AT);
        assertThat(event.getRetryCount()).isZero();
        assertThat(payload.get("customerId").asText()).isEqualTo("CUS-001");
        assertThat(payload.get("name").asText()).isEqualTo("John Smith");
        assertThat(payload.get("identification").asText()).isEqualTo("0102030405");
        assertThat(payload.get("status").asBoolean()).isTrue();
        assertThat(payload.get("occurredAt").asText()).isEqualTo("2026-06-18T10:00:00Z");
    }

    private Customer customer() {
        return Customer.builder()
                .name("John Smith")
                .gender("MALE")
                .age(32)
                .identification("0102030405")
                .address("123 Main Avenue")
                .phone("0999999999")
                .customerId("CUS-001")
                .password("encoded-secret")
                .status(true)
                .build();
    }
}
