package com.biojaveriana.api.observation.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservationRabbitConfig {

    @Value("${app.rabbitmq.exchange:observation.jobs.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.queue-names:observation.jobs}")
    private String queueName;

    @Value("${app.rabbitmq.response-queue:observation.jobs.response}")
    private String responseQueue;

    @Value("${app.rabbitmq.dead-letter-queue:observation.jobs.dlq}")
    private String dlqName;

    @Bean
    public DirectExchange observationExchange() {
        return new DirectExchange(exchange);
    }

    @Bean
    public Queue observationQueue() {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", exchange)
                .withArgument("x-message-ttl", 86400000)
                .build();
    }

    @Bean
    public Queue responseQueue() {
        return QueueBuilder.durable(responseQueue)
                .withArgument("x-dead-letter-exchange", exchange)
                .build();
    }

    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(dlqName).build();
    }

    @Bean
    public Binding binding(DirectExchange exchange, Queue observationQueue) {
        return BindingBuilder.bind(observationQueue).to(exchange).with("observation.jobs");
    }
}