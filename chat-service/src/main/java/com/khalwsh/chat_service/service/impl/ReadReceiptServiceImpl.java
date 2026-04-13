package com.khalwsh.chat_service.service.impl;

import com.khalwsh.chat_service.dto.response.UnreadCountResponse;
import com.khalwsh.chat_service.repository.MessageRepository;
import com.khalwsh.chat_service.service.ReadReceiptService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class ReadReceiptServiceImpl implements ReadReceiptService {

    private final StringRedisTemplate redisTemplate;
    private final MessageRepository messageRepository;

    // --- channel reads ---
    // key format: read:{channelId}:{userId} -> lastReadMessageId

    private String channelKey(String channelId, Integer userId) {
        return "read:" + channelId + ":" + userId;
    }

    @Override
    public void markAsRead(String channelId, Integer userId, String lastReadMessageId) {
        String key = channelKey(channelId, userId);
        moveForwardOnly(key, lastReadMessageId);
    }

    @Override
    public UnreadCountResponse getUnreadCount(String channelId, Integer userId) {
        String key = channelKey(channelId, userId);
        String lastReadMessageId = redisTemplate.opsForValue().get(key);

        long unreadCount;

        if (lastReadMessageId == null) {
            // never read anything — count everything
            unreadCount = messageRepository.countChannelMessagesAfter(
                    new ObjectId(channelId), ObjectId.getSmallestWithDate(new Date(0)));
        } else {
            unreadCount = messageRepository.countChannelMessagesAfter(
                    new ObjectId(channelId), new ObjectId(lastReadMessageId));
        }

        return UnreadCountResponse.builder()
                .unreadCount(unreadCount)
                .lastReadMessageId(lastReadMessageId)
                .build();
    }

    // --- thread reads (separate redis keys) ---
    // key format: read:thread:{threadId}:{userId} -> lastReadMessageId

    private String threadKey(String threadId, Integer userId) {
        return "read:thread:" + threadId + ":" + userId;
    }

    @Override
    public void markThreadAsRead(String threadId, Integer userId, String lastReadMessageId) {
        String key = threadKey(threadId, userId);
        moveForwardOnly(key, lastReadMessageId);
    }

    @Override
    public UnreadCountResponse getThreadUnreadCount(String threadId, Integer userId) {
        String key = threadKey(threadId, userId);
        String lastReadMessageId = redisTemplate.opsForValue().get(key);

        long unreadCount;

        if (lastReadMessageId == null) {
            // never read anything in this thread
            unreadCount = messageRepository.countThreadMessagesAfter(
                    new ObjectId(threadId), ObjectId.getSmallestWithDate(new Date(0)));
        } else {
            unreadCount = messageRepository.countThreadMessagesAfter(
                    new ObjectId(threadId), new ObjectId(lastReadMessageId));
        }

        return UnreadCountResponse.builder()
                .unreadCount(unreadCount)
                .lastReadMessageId(lastReadMessageId)
                .build();
    }

    // --- helpers ---

    // only move the cursor forward to avoid stale requests overwriting newer ones
    private void moveForwardOnly(String key, String newMessageId) {
        String script =
                "local current = redis.call('GET', KEYS[1]) " +
                        "if not current then " +
                        "  redis.call('SET', KEYS[1], ARGV[1]) " +
                        "  return 1 " +
                        "end " +
                        "if current < ARGV[1] then " +
                        "  redis.call('SET', KEYS[1], ARGV[1]) " +
                        "  return 1 " +
                        "end " +
                        "return 0";

        redisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(key),
                newMessageId
        );
    }
}
