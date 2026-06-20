package com.devsu.account.messaging;

import com.devsu.account.event.CustomerEvent;
import com.devsu.account.exception.InvalidCustomerEventException;
import com.devsu.account.service.CustomerSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerEventConsumer {

    private final CustomerEventMessageMapper messageMapper;
    private final CustomerSnapshotService snapshotService;

    @RabbitListener(queues = "${account.messaging.customer-events.queue}")
    public void consume(Message message) {
        try {
            CustomerEvent event = messageMapper.map(message);
            log.info(
                    "Processing customer event type={} customerId={} occurredAt={}",
                    event.type(),
                    event.customerId(),
                    event.occurredAt()
            );
            snapshotService.apply(event);
        } catch (InvalidCustomerEventException exception) {
            log.warn(
                    "Rejecting invalid customer event routingKey={}: {}",
                    message.getMessageProperties().getReceivedRoutingKey(),
                    exception.getMessage()
            );
            log.debug("Invalid customer event details", exception);
            throw new AmqpRejectAndDontRequeueException(exception.getMessage(), exception);
        }
    }
}
