package com.virtualoffice.chat_service.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Publishes NotificationEvents (e.g. @mentions) to notifications-service over RabbitMQ. */
@Service
@Slf4j
public class NotificationPublisher {

    private final RabbitTemplate notificationsRabbitTemplate;

    @Value("${notifications.exchange:notifications.exchange}")
    private String exchange;

    @Value("${notifications.routing.key:routing.key}")
    private String routingKey;

    public NotificationPublisher(@Qualifier("notificationsRabbitTemplate") RabbitTemplate notificationsRabbitTemplate) {
        this.notificationsRabbitTemplate = notificationsRabbitTemplate;
    }

    public void publish(NotificationType type, Map<String, Object> payload) {
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID().toString(), type, Instant.now(), payload);
        try {
            notificationsRabbitTemplate.convertAndSend(exchange, routingKey, event);
        } catch (Exception e) {
            log.error("Failed to publish notification {}: {}", type, e.getMessage());
        }
    }
}
