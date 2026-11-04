package com.biojaveriana.api.observation.rabbitmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.biojaveriana.api.observation.dto.ObservationMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ObservationMessageProducer {
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.rabbitmq.exchange:observation.jobs.exchange}")
    private String exchange;

    public void sendMessage(ObservationMessage message) {
        try {
            String body = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(exchange, "observation.jobs", body);
            log.info("Published observation job for observationId={}", message.getObservationId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize observation message for observationId={}", message.getObservationId(), e);
            throw new RuntimeException("Failed to serialize observation message", e);
        }
    }
}
