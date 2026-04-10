package com.khalwsh.chat_service.config;

import com.khalwsh.chat_service.model.Channel;
import com.khalwsh.chat_service.model.ChatThread;
import com.khalwsh.chat_service.repository.ChannelRepository;
import com.khalwsh.chat_service.repository.ThreadRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

// checks SUBSCRIBE frames to make sure the user belongs to the channel/thread
@Component
@RequiredArgsConstructor
public class WebSocketSubscriptionInterceptor implements ChannelInterceptor {

    private final ChannelRepository channelRepository;
    private final ThreadRepository threadRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message; // not a subscribe, pass through
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            return message;
        }

        Integer userId = getUserIdFromSession(accessor);
        if (userId == null) {
            throw new IllegalStateException("no userId in WebSocket session");
        }

        // /topic/channel/{channelId} or /topic/channel/{channelId}/typing
        if (destination.startsWith("/topic/channel/")) {
            String channelId = extractId(destination, "/topic/channel/");
            validateChannelMembership(channelId, userId);
        }

        // /topic/thread/{threadId} or /topic/thread/{threadId}/typing
        if (destination.startsWith("/topic/thread/")) {
            String threadId = extractId(destination, "/topic/thread/");
            validateThreadAccess(threadId, userId);
        }

        return message;
    }

    private void validateChannelMembership(String channelId, Integer userId) {
        Optional<Channel> channelOpt = channelRepository.findById(new ObjectId(channelId));
        if (channelOpt.isEmpty()) {
            throw new IllegalArgumentException("channel not found: " + channelId);
        }
        if (!channelOpt.get().getMembers().contains(userId)) {
            throw new IllegalArgumentException("not a member of channel: " + channelId);
        }
    }

    private void validateThreadAccess(String threadId, Integer userId) {
        // check thread access via parent channel
        Optional<ChatThread> threadOpt = threadRepository.findActiveById(new ObjectId(threadId));
        if (threadOpt.isEmpty()) {
            throw new IllegalArgumentException("thread not found: " + threadId);
        }
        String channelId = threadOpt.get().getChannelId().toHexString();
        validateChannelMembership(channelId, userId);
    }

    // grab the id part from /topic/channel/abc123 or /topic/channel/abc123/typing
    private String extractId(String destination, String prefix) {
        String remainder = destination.substring(prefix.length());
        int slashIndex = remainder.indexOf('/');
        return slashIndex > 0 ? remainder.substring(0, slashIndex) : remainder;
    }

    private Integer getUserIdFromSession(StompHeaderAccessor accessor) {
        Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
        if (sessionAttrs == null) return null;
        return (Integer) sessionAttrs.get("userId");
    }
}
