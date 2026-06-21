package com.devsu.account.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "account.messaging.customer-events")
public class CustomerEventMessagingProperties {

    private String exchange;
    private String queue;
    private String routingKey;
    private String deadLetterExchange;
    private String deadLetterQueue;
    private String deadLetterRoutingKey;
    private int maximumAttempts;
    private long initialInterval;
    private double multiplier;
    private long maximumInterval;
}
