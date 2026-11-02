package com.biojaveriana.api.job;

import java.util.List;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    private static final String EXCHANGE_NAME = "outbox.exchange";

    @Value("${app.rabbitmq.queue-names:default}")
    private List<String> queueNames;

    @Bean
    public DirectExchange outboxExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue[] queues() {
        return queueNames.stream()
            .map(name -> new Queue(name, true))
            .toArray(Queue[]::new);
    }

    @Bean
    public Binding[] bindings(DirectExchange exchange, Queue... queues) {
        return java.util.Arrays.stream(queues)
            .map(queue -> BindingBuilder.bind(queue).to(exchange).with(queue.getName()))
            .toArray(Binding[]::new);
    }
}
