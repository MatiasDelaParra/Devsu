package com.devsu.customer.outbox;

import com.devsu.customer.config.CustomerEventMessagingProperties;
import com.devsu.customer.event.CustomerEventType;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final CustomerEventMessagingProperties messagingProperties;
    private final Clock clock;

    @Scheduled(
            fixedDelayString = "${customer.messaging.publisher.fixed-delay:5000}",
            initialDelayString = "${customer.messaging.publisher.initial-delay:5000}"
    )
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxEventStatus.PENDING,
                PageRequest.of(0, messagingProperties.getPublisher().getBatchSize())
        );

        events.forEach(this::publish);
    }

    private void publish(OutboxEvent event) {
        try {
            CustomerEventType eventType = CustomerEventType.valueOf(event.getEventType());
            rabbitTemplate.convertAndSend(
                    messagingProperties.getExchange(),
                    messagingProperties.routingKeyFor(eventType),
                    event.getPayload()
            );
            event.markPublished(clock.instant());
        } catch (Exception exception) {
            event.registerFailure(
                    errorMessage(exception),
                    messagingProperties.getPublisher().getMaximumRetries()
            );
            log.warn(
                    "No se pudo publicar el evento de outbox {}. Intento {} de {}: {}",
                    event.getId(),
                    event.getRetryCount(),
                    messagingProperties.getPublisher().getMaximumRetries(),
                    errorMessage(exception)
            );
            log.debug("Error completo al publicar el evento de outbox {}", event.getId(), exception);
        }
    }

    private String errorMessage(Exception exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }
}
