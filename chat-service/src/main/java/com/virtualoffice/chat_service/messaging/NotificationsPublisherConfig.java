package com.virtualoffice.chat_service.messaging;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** A dedicated JSON RabbitTemplate for publishing NotificationEvents (kept separate from the
 *  chat-service consumer setup to avoid converter/type conflicts). */
@Configuration
public class NotificationsPublisherConfig {

    @Bean
    public RabbitTemplate notificationsRabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }
}
