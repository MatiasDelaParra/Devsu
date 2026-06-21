package com.devsu.customer.outbox;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import com.devsu.customer.config.CustomerEventMessagingProperties;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class RabbitMqCustomerEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private RabbitMqCustomerEventPublisher publisher;

    @BeforeEach
    void setUp() {
        CustomerEventMessagingProperties properties = new CustomerEventMessagingProperties();
        properties.getPublisher().setConfirmationTimeout(100);
        publisher = new RabbitMqCustomerEventPublisher(rabbitTemplate, properties);
    }

    @Test
    void waitsForBrokerAcknowledgement() {
        OutboxEvent event = event();
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(4);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(
                eq("customer.events"),
                eq("customer.created"),
                eq(event.getPayload()),
                any(MessagePostProcessor.class),
                any(CorrelationData.class)
        );

        publisher.publish(event);

        verify(rabbitTemplate).convertAndSend(
                eq("customer.events"),
                eq("customer.created"),
                eq(event.getPayload()),
                any(MessagePostProcessor.class),
                any(CorrelationData.class)
        );
    }

    @Test
    void rejectsNegativeBrokerAcknowledgement() {
        OutboxEvent event = event();
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(4);
            correlation.getFuture().complete(new CorrelationData.Confirm(false, "nack"));
            return null;
        }).when(rabbitTemplate).convertAndSend(
                any(String.class),
                any(String.class),
                any(),
                any(MessagePostProcessor.class),
                any(CorrelationData.class)
        );

        assertThatThrownBy(() -> publisher.publish(event))
                .isInstanceOf(AmqpException.class)
                .hasMessageContaining("rechazó");
    }

    private OutboxEvent event() {
        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("CUSTOMER")
                .aggregateId("CUS-001")
                .eventType("CUSTOMER_CREATED")
                .payload("{\"customerId\":\"CUS-001\"}")
                .status(OutboxEventStatus.PENDING)
                .createdAt(Instant.parse("2026-06-20T12:00:00Z"))
                .build();
    }
}
