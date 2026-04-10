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
        void shouldSetLastReadMessageId() {
            String channelId = new ObjectId().toHexString();
            ObjectId messageIdObj = new ObjectId();
            String messageId = messageIdObj.toHexString();

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("read:" + channelId + ":10")).thenReturn(null);

            readReceiptService.markAsRead(channelId, 10, messageId);

            verify(valueOperations).set("read:" + channelId + ":10", messageId);
        }

        @Test
        void shouldMoveForwardOnly() {
            String channelId = new ObjectId().toHexString();
            // create two ObjectIds where newer > older in timestamp
            ObjectId older = new ObjectId();
            // small delay to ensure different timestamps
            ObjectId newer = new ObjectId();

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("read:" + channelId + ":10")).thenReturn(newer.toHexString());

            // trying to move cursor backward — should be ignored
            readReceiptService.markAsRead(channelId, 10, older.toHexString());

            verify(valueOperations, never()).set(anyString(), eq(older.toHexString()));
        }

        @Test
        void shouldMoveForwardWhenNewerIdProvided() {
            String channelId = new ObjectId().toHexString();
            ObjectId older = new ObjectId();
            ObjectId newer = new ObjectId();

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("read:" + channelId + ":10")).thenReturn(older.toHexString());

            readReceiptService.markAsRead(channelId, 10, newer.toHexString());

            verify(valueOperations).set("read:" + channelId + ":10", newer.toHexString());
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
        void shouldSetLastReadForThread() {
            String threadId = new ObjectId().toHexString();
            ObjectId messageIdObj = new ObjectId();
            String messageId = messageIdObj.toHexString();

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("read:thread:" + threadId + ":10")).thenReturn(null);

            readReceiptService.markThreadAsRead(threadId, 10, messageId);

            verify(valueOperations).set("read:thread:" + threadId + ":10", messageId);
        }

        @Test
        void shouldUseThreadKeyPrefix() {
            String threadId = new ObjectId().toHexString();
            ObjectId messageIdObj = new ObjectId();

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("read:thread:" + threadId + ":10")).thenReturn(null);

            readReceiptService.markThreadAsRead(threadId, 10, messageIdObj.toHexString());

            // verify the correct key prefix is used (not channel key)
            verify(valueOperations).get("read:thread:" + threadId + ":10");
        }

        @Test
        void shouldMoveForwardOnlyForThreads() {
            String threadId = new ObjectId().toHexString();
            ObjectId older = new ObjectId();
            ObjectId newer = new ObjectId();

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("read:thread:" + threadId + ":10")).thenReturn(newer.toHexString());

            readReceiptService.markThreadAsRead(threadId, 10, older.toHexString());

            verify(valueOperations, never()).set(anyString(), eq(older.toHexString()));
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
