package com.devsu.account.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CustomerEventMessagingProperties.class)
public class RabbitMqConfig {

    @Bean
    public TopicExchange customerEventsExchange(CustomerEventMessagingProperties properties) {
        return new TopicExchange(properties.getExchange(), true, false);
    }

    @Bean
    public Queue accountCustomerEventsQueue(CustomerEventMessagingProperties properties) {
        return new Queue(properties.getQueue(), true);
    }

    @Bean
    public Binding accountCustomerEventsBinding(
            Queue accountCustomerEventsQueue,
            TopicExchange customerEventsExchange,
            CustomerEventMessagingProperties properties
    ) {
        return BindingBuilder.bind(accountCustomerEventsQueue)
                .to(customerEventsExchange)
                .with(properties.getRoutingKey());
    }

    @Bean
    public Jackson2JsonMessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
