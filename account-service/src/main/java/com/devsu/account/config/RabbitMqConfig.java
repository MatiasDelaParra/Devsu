package com.devsu.account.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
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
        return QueueBuilder.durable(properties.getQueue())
                .deadLetterExchange(properties.getDeadLetterExchange())
                .deadLetterRoutingKey(properties.getDeadLetterRoutingKey())
                .build();
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
    public TopicExchange customerEventsDeadLetterExchange(
            CustomerEventMessagingProperties properties
    ) {
        return new TopicExchange(properties.getDeadLetterExchange(), true, false);
    }

    @Bean
    public Queue accountCustomerEventsDeadLetterQueue(
            CustomerEventMessagingProperties properties
    ) {
        return QueueBuilder.durable(properties.getDeadLetterQueue()).build();
    }

    @Bean
    public Binding accountCustomerEventsDeadLetterBinding(
            Queue accountCustomerEventsDeadLetterQueue,
            TopicExchange customerEventsDeadLetterExchange,
            CustomerEventMessagingProperties properties
    ) {
        return BindingBuilder.bind(accountCustomerEventsDeadLetterQueue)
                .to(customerEventsDeadLetterExchange)
                .with(properties.getDeadLetterRoutingKey());
    }

    @Bean(name = "rabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            CustomerEventMessagingProperties properties
    ) {
        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(properties.getMaximumAttempts())
                .backOffOptions(
                        properties.getInitialInterval(),
                        properties.getMultiplier(),
                        properties.getMaximumInterval()
                )
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build());
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public Jackson2JsonMessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
