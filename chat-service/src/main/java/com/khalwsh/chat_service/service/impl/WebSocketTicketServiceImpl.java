package com.khalwsh.chat_service.service.impl;

import com.khalwsh.chat_service.service.WebSocketTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WebSocketTicketServiceImpl implements WebSocketTicketService {

    private final StringRedisTemplate redisTemplate;

    // 60s should be enough to open the connection
    private static final Duration TICKET_TTL = Duration.ofSeconds(60);
    private static final String KEY_PREFIX = "ws-ticket:";

    @Override
    public String createTicket(Integer userId) {
        String ticket = UUID.randomUUID().toString();
        // ws-ticket:{ticket} -> userId
        redisTemplate.opsForValue().set(KEY_PREFIX + ticket, userId.toString(), TICKET_TTL);
        return ticket;
    }

    @Override
    public Integer validateAndConsumeTicket(String ticket) {
        if (ticket == null) return null;

        String key = KEY_PREFIX + ticket;
        // get + delete in one shot so it can't be reused
        String userId = redisTemplate.opsForValue().getAndDelete(key);
        if (userId == null) return null;

        return Integer.parseInt(userId);
    }
}
