package com.devsu.account.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "account.messaging.customer-events")
public class CustomerEventMessagingProperties {

    private String exchange = "customer.events";
    private String queue = "account.customer-events.queue";
    private String routingKey = "customer.#";
}
