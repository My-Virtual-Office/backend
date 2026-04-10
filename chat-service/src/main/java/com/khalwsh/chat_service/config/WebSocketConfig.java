package com.khalwsh.chat_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketHandshakeInterceptor handshakeInterceptor;
    private final WebSocketSubscriptionInterceptor subscriptionInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // subscribers listen on /topic/channel/{id} and /topic/thread/{id}
        config.enableSimpleBroker("/topic");
        // senders hit /app/chat/send and /app/chat/typing
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // connect with ?ticket={ticket}
        registry.addEndpoint("/api/chat/connect")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // check channel/thread membership on subscribe
        registration.interceptors(subscriptionInterceptor);
    }
}
