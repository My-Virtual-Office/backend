package com.virtualoffice.workspace.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Publishes NotificationEvents to the notifications exchange (invite emails, etc.). */
@Service
@Slf4j
public class NotificationPublisher {

    // Reuses the auto-configured RabbitTemplate (JSON converter from RabbitConfig).
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public NotificationPublisher(RabbitTemplate rabbitTemplate,
                                 @Value("${notifications.exchange}") String exchange,
                                 @Value("${notifications.routing.key}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void publish(NotificationType type, Map<String, Object> payload) {
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID().toString(), type, Instant.now(), payload);
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
        } catch (Exception e) {
            log.error("Failed to publish notification {}: {}", type, e.getMessage());
        }
    }
}
