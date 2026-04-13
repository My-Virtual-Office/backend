package com.khalwsh.chat_service.service.impl;

import com.khalwsh.chat_service.dto.response.UnreadCountResponse;
import com.khalwsh.chat_service.repository.MessageRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadReceiptServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private ReadReceiptServiceImpl readReceiptService;

    // ────────────────────────────────────────
    // channel read receipts
    // ────────────────────────────────────────

    @Nested
    class ChannelMarkAsRead {

        @Test
        void shouldCallLuaScriptWithCorrectKey() {
            String channelId = new ObjectId().toHexString();
            String messageId = new ObjectId().toHexString();

            readReceiptService.markAsRead(channelId, 10, messageId);

            // lua script should be called with the correct key and message id
            verify(redisTemplate).execute(
                    any(RedisScript.class),
                    eq(List.of("read:" + channelId + ":10")),
                    eq(messageId)
            );
        }

        @Test
        void shouldFormCorrectRedisKeyForChannel() {
            String channelId = new ObjectId().toHexString();
            String messageId = new ObjectId().toHexString();

            readReceiptService.markAsRead(channelId, 42, messageId);

            verify(redisTemplate).execute(
                    any(RedisScript.class),
                    eq(List.of("read:" + channelId + ":42")),
                    eq(messageId)
            );
        }
    }

    @Nested
    class ChannelUnreadCount {

        @Test
        void shouldReturnUnreadCountWithLastReadPosition() {
            String channelId = new ObjectId().toHexString();
            ObjectId lastRead = new ObjectId();

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("read:" + channelId + ":10")).thenReturn(lastRead.toHexString());
            when(messageRepository.countChannelMessagesAfter(eq(new ObjectId(channelId)), eq(lastRead)))
                    .thenReturn(5L);

            UnreadCountResponse response = readReceiptService.getUnreadCount(channelId, 10);

            assertThat(response.getUnreadCount()).isEqualTo(5);
            assertThat(response.getLastReadMessageId()).isEqualTo(lastRead.toHexString());
        }

        @Test
        void shouldCountAllMessagesWhenNeverRead() {
            String channelId = new ObjectId().toHexString();

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("read:" + channelId + ":10")).thenReturn(null);
            when(messageRepository.countChannelMessagesAfter(eq(new ObjectId(channelId)), any(ObjectId.class)))
                    .thenReturn(42L);

            UnreadCountResponse response = readReceiptService.getUnreadCount(channelId, 10);

            assertThat(response.getUnreadCount()).isEqualTo(42);
            assertThat(response.getLastReadMessageId()).isNull();
        }
    }

    // ────────────────────────────────────────
    // thread read receipts
    // ────────────────────────────────────────

    @Nested
    class ThreadMarkAsRead {

        @Test
        void shouldCallLuaScriptForThread() {
            String threadId = new ObjectId().toHexString();
            String messageId = new ObjectId().toHexString();

            readReceiptService.markThreadAsRead(threadId, 10, messageId);

            verify(redisTemplate).execute(
                    any(RedisScript.class),
                    eq(List.of("read:thread:" + threadId + ":10")),
                    eq(messageId)
            );
        }

        @Test
        void shouldUseThreadKeyPrefix() {
            String threadId = new ObjectId().toHexString();
            String messageId = new ObjectId().toHexString();

            readReceiptService.markThreadAsRead(threadId, 10, messageId);

            // verify the correct key prefix is used (not channel key)
            verify(redisTemplate).execute(
                    any(RedisScript.class),
                    eq(List.of("read:thread:" + threadId + ":10")),
                    eq(messageId)
            );
        }

        @Test
        void shouldNotUseChannelKeyForThreadMark() {
            String threadId = new ObjectId().toHexString();
            String messageId = new ObjectId().toHexString();

            readReceiptService.markThreadAsRead(threadId, 10, messageId);

            // should NOT call with a channel-style key
            verify(redisTemplate, never()).execute(
                    any(RedisScript.class),
                    eq(List.of("read:" + threadId + ":10")),
                    eq(messageId)
            );
        }
    }

    @Nested
    class ThreadUnreadCount {

        @Test
        void shouldReturnThreadUnreadCount() {
            String threadId = new ObjectId().toHexString();
            ObjectId lastRead = new ObjectId();

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("read:thread:" + threadId + ":10")).thenReturn(lastRead.toHexString());
            when(messageRepository.countThreadMessagesAfter(eq(new ObjectId(threadId)), eq(lastRead)))
                    .thenReturn(3L);

            UnreadCountResponse response = readReceiptService.getThreadUnreadCount(threadId, 10);

            assertThat(response.getUnreadCount()).isEqualTo(3);
            assertThat(response.getLastReadMessageId()).isEqualTo(lastRead.toHexString());
        }

        @Test
        void shouldCountAllThreadMessagesWhenNeverRead() {
            String threadId = new ObjectId().toHexString();

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("read:thread:" + threadId + ":10")).thenReturn(null);
            when(messageRepository.countThreadMessagesAfter(eq(new ObjectId(threadId)), any(ObjectId.class)))
                    .thenReturn(15L);

            UnreadCountResponse response = readReceiptService.getThreadUnreadCount(threadId, 10);

            assertThat(response.getUnreadCount()).isEqualTo(15);
            assertThat(response.getLastReadMessageId()).isNull();
        }
    }
}
