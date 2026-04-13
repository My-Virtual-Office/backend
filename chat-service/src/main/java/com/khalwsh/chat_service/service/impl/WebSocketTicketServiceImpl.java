package com.khalwsh.chat_service.service.impl;

import com.khalwsh.chat_service.service.WebSocketTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebSocketTicketServiceImpl implements WebSocketTicketService {

    private final StringRedisTemplate redisTemplate;

    // 60s should be enough to open the connection
    private static final Duration TICKET_TTL = Duration.ofSeconds(60);
    private static final String KEY_PREFIX = "ws-ticket:";

    @Override
    public String createTicket(Integer userId, String role) {
        String ticket = UUID.randomUUID().toString();
        // store as "userId:role" so the handshake can recover both
        String value = userId + ":" + (role != null ? role : "USER");
        redisTemplate.opsForValue().set(KEY_PREFIX + ticket, value, TICKET_TTL);
        return ticket;
    }

    @Override
    public Map<String, Object> validateAndConsumeTicket(String ticket) {
        if (ticket == null) return null;

        String key = KEY_PREFIX + ticket;
        // get + delete in one shot so it can't be reused
        String value = redisTemplate.opsForValue().getAndDelete(key);
        if (value == null) return null;

        // value format: "userId:role"
        String[] parts = value.split(":", 2);
        Integer userId = Integer.parseInt(parts[0]);
        String role = parts.length > 1 ? parts[1] : "USER";
        return Map.of("userId", userId, "userRole", role);
    }
}
