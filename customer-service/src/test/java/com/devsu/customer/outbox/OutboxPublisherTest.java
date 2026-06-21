package com.devsu.customer.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devsu.customer.config.CustomerEventMessagingProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    private static final Instant PUBLISHED_AT = Instant.parse("2026-06-18T10:05:00Z");

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private CustomerEventPublisher customerEventPublisher;

    private CustomerEventMessagingProperties properties;
    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        properties = new CustomerEventMessagingProperties();
        properties.getPublisher().setBatchSize(10);
        properties.getPublisher().setMaximumRetries(3);
        publisher = new OutboxPublisher(
                outboxEventRepository,
                customerEventPublisher,
                properties,
                Clock.fixed(PUBLISHED_AT, ZoneOffset.UTC)
        );
    }

    @Test
    void publishesPendingEventAndMarksItAsPublished() {
        OutboxEvent event = event();
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                eq(OutboxEventStatus.PENDING),
                any(Pageable.class)
        )).thenReturn(List.of(event));

        publisher.publishPendingEvents();

        verify(customerEventPublisher).publish(event);
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isEqualTo(PUBLISHED_AT);
        assertThat(event.getLastError()).isNull();
    }

    @Test
    void incrementsRetryCountWhenPublishingFails() {
        OutboxEvent event = event();
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                eq(OutboxEventStatus.PENDING),
                any(Pageable.class)
        )).thenReturn(List.of(event));
        doThrow(new AmqpException("RabbitMQ no disponible"))
                .when(customerEventPublisher)
                .publish(event);

        publisher.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getLastError()).isEqualTo("RabbitMQ no disponible");
        assertThat(event.getPublishedAt()).isNull();
    }

    @Test
    void marksEventAsFailedAfterMaximumRetryCount() {
        OutboxEvent event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("CUSTOMER")
                .aggregateId("CUS-001")
                .eventType("CUSTOMER_CREATED")
                .payload("{\"customerId\":\"CUS-001\"}")
                .status(OutboxEventStatus.PENDING)
                .createdAt(Instant.parse("2026-06-18T10:00:00Z"))
                .retryCount(2)
                .build();
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                eq(OutboxEventStatus.PENDING),
                any(Pageable.class)
        )).thenReturn(List.of(event));
        doThrow(new AmqpException("RabbitMQ no disponible"))
                .when(customerEventPublisher)
                .publish(event);

        publisher.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(3);
        assertThat(event.getLastError()).isEqualTo("RabbitMQ no disponible");
    }

    private OutboxEvent event() {
        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("CUSTOMER")
                .aggregateId("CUS-001")
                .eventType("CUSTOMER_CREATED")
                .payload("{\"customerId\":\"CUS-001\"}")
                .status(OutboxEventStatus.PENDING)
                .createdAt(Instant.parse("2026-06-18T10:00:00Z"))
                .retryCount(0)
                .build();
    }
}
