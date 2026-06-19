package com.devsu.customer.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(CustomerEventMessagingProperties.class)
public class RabbitMqConfig {

    @Bean
    public TopicExchange customerEventsExchange(CustomerEventMessagingProperties properties) {
        return new TopicExchange(properties.getExchange(), true, false);
    }
}
