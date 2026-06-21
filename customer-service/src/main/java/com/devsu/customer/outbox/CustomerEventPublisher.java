package com.devsu.customer.outbox;

public interface CustomerEventPublisher {

    void publish(OutboxEvent event);
}
