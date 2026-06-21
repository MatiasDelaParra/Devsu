package com.devsu.customer.config;

import com.devsu.customer.event.CustomerEventType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "customer.messaging")
public class CustomerEventMessagingProperties {

    private String exchange = "customer.events";
    private RoutingKeys routingKeys = new RoutingKeys();
    private Publisher publisher = new Publisher();

    public String routingKeyFor(CustomerEventType eventType) {
        return switch (eventType) {
            case CUSTOMER_CREATED -> routingKeys.getCreated();
            case CUSTOMER_UPDATED -> routingKeys.getUpdated();
            case CUSTOMER_STATUS_CHANGED -> routingKeys.getStatusChanged();
            case CUSTOMER_DELETED -> routingKeys.getDeleted();
        };
    }

    @Getter
    @Setter
    public static class RoutingKeys {

        private String created = "customer.created";
        private String updated = "customer.updated";
        private String statusChanged = "customer.status-changed";
        private String deleted = "customer.deleted";
    }

    @Getter
    @Setter
    public static class Publisher {

        private int batchSize = 50;
        private int maximumRetries = 5;
        private long fixedDelay = 5000;
        private long initialDelay = 5000;
        private long confirmationTimeout = 5000;
    }
}
