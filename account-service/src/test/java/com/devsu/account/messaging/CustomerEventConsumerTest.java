package com.devsu.account.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.devsu.account.event.CustomerEvent;
import com.devsu.account.event.CustomerEventType;
import com.devsu.account.service.CustomerSnapshotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

class CustomerEventConsumerTest {

    private final CustomerSnapshotService snapshotService =
            org.mockito.Mockito.mock(CustomerSnapshotService.class);
    private final CustomerEventConsumer consumer = new CustomerEventConsumer(
            new CustomerEventMessageMapper(new ObjectMapper().registerModule(new JavaTimeModule())),
            snapshotService
    );

    @Test
    void deserializesAndProcessesValidCustomerEvent() {
        String json = """
                {
                  "customerId": "customer-1",
                  "name": "Jane Doe",
                  "identification": "1234567890",
                  "status": true,
                  "occurredAt": "2026-06-20T12:00:00Z"
                }
                """;
        MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey("customer.created");
        properties.setHeader("eventId", "11111111-1111-1111-1111-111111111111");
        Message message = new Message(json.getBytes(StandardCharsets.UTF_8), properties);

        consumer.consume(message);

        ArgumentCaptor<CustomerEvent> eventCaptor = ArgumentCaptor.forClass(CustomerEvent.class);
        verify(snapshotService).apply(eventCaptor.capture());
        CustomerEvent event = eventCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(event.type())
                .isEqualTo(CustomerEventType.CUSTOMER_CREATED);
        org.assertj.core.api.Assertions.assertThat(event.eventId())
                .isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        org.assertj.core.api.Assertions.assertThat(event.customerId()).isEqualTo("customer-1");
    }

    @Test
    void rejectsMalformedCustomerEventWithoutCallingService() {
        MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey("customer.created");
        Message message = new Message("{invalid".getBytes(StandardCharsets.UTF_8), properties);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> consumer.consume(message))
                .isInstanceOf(org.springframework.amqp.AmqpRejectAndDontRequeueException.class);
        verify(snapshotService, org.mockito.Mockito.never()).apply(any());
    }
}
