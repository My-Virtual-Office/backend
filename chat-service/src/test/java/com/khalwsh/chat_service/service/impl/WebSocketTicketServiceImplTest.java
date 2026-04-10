package com.khalwsh.chat_service.service.impl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketTicketServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private WebSocketTicketServiceImpl webSocketTicketService;

    // ────────────────────────────────────────
    // createTicket
    // ────────────────────────────────────────

    @Nested
    class CreateTicket {

        @Test
        void shouldReturnNonNullTicket() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            String ticket = webSocketTicketService.createTicket(10);

            assertThat(ticket).isNotNull().isNotBlank();
        }

        @Test
        void shouldStoreTicketInRedisWithTtl() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            String ticket = webSocketTicketService.createTicket(10);

            verify(valueOperations).set(
                    eq("ws-ticket:" + ticket),
                    eq("10"),
                    eq(Duration.ofSeconds(60))
            );
        }

        @Test
        void shouldGenerateUniqueTicketsPerCall() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            String ticket1 = webSocketTicketService.createTicket(10);
            String ticket2 = webSocketTicketService.createTicket(10);

            assertThat(ticket1).isNotEqualTo(ticket2);
        }

        @Test
        void shouldStoreUserIdAsString() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            webSocketTicketService.createTicket(42);

            verify(valueOperations).set(
                    anyString(),
                    eq("42"),
                    any(Duration.class)
            );
        }
    }

    // ────────────────────────────────────────
    // validateAndConsumeTicket
    // ────────────────────────────────────────

    @Nested
    class ValidateAndConsumeTicket {

        @Test
        void shouldReturnUserIdForValidTicket() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.getAndDelete("ws-ticket:valid-ticket")).thenReturn("10");

            Integer userId = webSocketTicketService.validateAndConsumeTicket("valid-ticket");

            assertThat(userId).isEqualTo(10);
        }

        @Test
        void shouldReturnNullForInvalidTicket() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.getAndDelete("ws-ticket:invalid-ticket")).thenReturn(null);

            Integer userId = webSocketTicketService.validateAndConsumeTicket("invalid-ticket");

            assertThat(userId).isNull();
        }

        @Test
        void shouldReturnNullForNullTicket() {
            Integer userId = webSocketTicketService.validateAndConsumeTicket(null);

            assertThat(userId).isNull();
            // should not even touch Redis
            verifyNoInteractions(redisTemplate);
        }

        @Test
        void shouldDeleteTicketOnConsumption() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.getAndDelete("ws-ticket:one-time")).thenReturn("10");

            webSocketTicketService.validateAndConsumeTicket("one-time");

            // getAndDelete atomically gets and removes — this IS the consumption
            verify(valueOperations).getAndDelete("ws-ticket:one-time");
        }

        @Test
        void shouldNotReturnUserIdOnSecondUse() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            // first call succeeds
            when(valueOperations.getAndDelete("ws-ticket:used-ticket"))
                    .thenReturn("10")   // first call
                    .thenReturn(null);  // second call — ticket already consumed

            Integer first = webSocketTicketService.validateAndConsumeTicket("used-ticket");
            Integer second = webSocketTicketService.validateAndConsumeTicket("used-ticket");

            assertThat(first).isEqualTo(10);
            assertThat(second).isNull();
        }
    }
}
