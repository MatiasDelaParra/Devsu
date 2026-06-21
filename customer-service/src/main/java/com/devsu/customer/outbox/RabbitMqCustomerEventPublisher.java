package com.devsu.customer.outbox;

import com.devsu.customer.config.CustomerEventMessagingProperties;
import com.devsu.customer.event.CustomerEventType;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitMqCustomerEventPublisher implements CustomerEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final CustomerEventMessagingProperties messagingProperties;

    @Override
    public void publish(OutboxEvent event) {
        CustomerEventType eventType = CustomerEventType.valueOf(event.getEventType());
        CorrelationData correlation = new CorrelationData(event.getId().toString());

        rabbitTemplate.convertAndSend(
                messagingProperties.getExchange(),
                messagingProperties.routingKeyFor(eventType),
                event.getPayload(),
                message -> {
                    message.getMessageProperties().setHeader("eventId", event.getId().toString());
                    message.getMessageProperties().setHeader("eventType", event.getEventType());
                    return message;
                },
                correlation
        );

        try {
            CorrelationData.Confirm confirm = correlation.getFuture().get(
                    messagingProperties.getPublisher().getConfirmationTimeout(),
                    TimeUnit.MILLISECONDS
            );

            if (!confirm.isAck()) {
                throw new AmqpException("RabbitMQ rechazó el evento: " + confirm.getReason());
            }

            var returned = correlation.getReturned();

            if (returned != null) {
                throw new AmqpException(
                        "RabbitMQ devolvió el evento sin enrutar: " + returned.getReplyText()
                );
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AmqpException("Se interrumpió la confirmación de RabbitMQ", exception);
        } catch (java.util.concurrent.ExecutionException
                 | java.util.concurrent.TimeoutException exception) {
            throw new AmqpException("No se recibió confirmación de RabbitMQ", exception);
        }
    }
}
