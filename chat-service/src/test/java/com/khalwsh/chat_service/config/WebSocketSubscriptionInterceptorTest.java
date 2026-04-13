package com.khalwsh.chat_service.config;

import com.khalwsh.chat_service.model.Channel;
import com.khalwsh.chat_service.model.ChannelType;
import com.khalwsh.chat_service.model.ChatThread;
import com.khalwsh.chat_service.repository.ChannelRepository;
import com.khalwsh.chat_service.repository.ThreadRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketSubscriptionInterceptorTest {

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private ThreadRepository threadRepository;

    @Mock
    private MessageChannel messageChannel;

    @InjectMocks
    private WebSocketSubscriptionInterceptor interceptor;

    private Message<?> buildSubscribeMessage(String destination, Integer userId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        if (userId != null) {
            Map<String, Object> sessionAttrs = new HashMap<>();
            sessionAttrs.put("userId", userId);
            accessor.setSessionAttributes(sessionAttrs);
        }
        accessor.setSubscriptionId("sub-1");
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<?> buildNonSubscribeMessage() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/chat/send");
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("userId", 10);
        accessor.setSessionAttributes(sessionAttrs);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    // ────────────────────────────────────────
    // pass-through for non-SUBSCRIBE frames
    // ────────────────────────────────────────

    @Nested
    class PassThrough {

        @Test
        void shouldPassThroughNonSubscribeMessages() {
            Message<?> msg = buildNonSubscribeMessage();

            Message<?> result = interceptor.preSend(msg, messageChannel);

            assertThat(result).isNotNull();
            verifyNoInteractions(channelRepository);
            verifyNoInteractions(threadRepository);
        }

        @Test
        void shouldPassThroughWhenDestinationIsNull() {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
            accessor.setDestination(null);
            Map<String, Object> sessionAttrs = new HashMap<>();
            sessionAttrs.put("userId", 10);
            accessor.setSessionAttributes(sessionAttrs);
            accessor.setSubscriptionId("sub-1");
            Message<?> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            Message<?> result = interceptor.preSend(msg, messageChannel);

            assertThat(result).isNotNull();
        }

        @Test
        void shouldPassThroughForUnrelatedDestinations() {
            Message<?> msg = buildSubscribeMessage("/user/queue/errors", 10);

            Message<?> result = interceptor.preSend(msg, messageChannel);

            assertThat(result).isNotNull();
            verifyNoInteractions(channelRepository);
        }
    }

    // ────────────────────────────────────────
    // channel subscription validation
    // ────────────────────────────────────────

    @Nested
    class ChannelSubscription {

        @Test
        void shouldAllowMemberToSubscribe() {
            ObjectId channelId = new ObjectId();
            Channel channel = Channel.builder()
                    .id(channelId)
                    .type(ChannelType.GROUP)
                    .members(List.of(10, 20))
                    .build();
            when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));

            Message<?> msg = buildSubscribeMessage("/topic/channel/" + channelId.toHexString(), 10);
            Message<?> result = interceptor.preSend(msg, messageChannel);

            assertThat(result).isNotNull();
        }

        @Test
        void shouldAllowMemberToSubscribeToTyping() {
            ObjectId channelId = new ObjectId();
            Channel channel = Channel.builder()
                    .id(channelId)
                    .type(ChannelType.GROUP)
                    .members(List.of(10))
                    .build();
            when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));

            Message<?> msg = buildSubscribeMessage("/topic/channel/" + channelId.toHexString() + "/typing", 10);
            Message<?> result = interceptor.preSend(msg, messageChannel);

            assertThat(result).isNotNull();
        }

        @Test
        void shouldRejectNonMember() {
            ObjectId channelId = new ObjectId();
            Channel channel = Channel.builder()
                    .id(channelId)
                    .type(ChannelType.GROUP)
                    .members(List.of(20, 30))
                    .build();
            when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));

            Message<?> msg = buildSubscribeMessage("/topic/channel/" + channelId.toHexString(), 10);

            assertThatThrownBy(() -> interceptor.preSend(msg, messageChannel))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not a member of channel");
        }

        @Test
        void shouldRejectIfChannelNotFound() {
            ObjectId channelId = new ObjectId();
            when(channelRepository.findById(channelId)).thenReturn(Optional.empty());

            Message<?> msg = buildSubscribeMessage("/topic/channel/" + channelId.toHexString(), 10);

            assertThatThrownBy(() -> interceptor.preSend(msg, messageChannel))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("channel not found");
        }

        @Test
        void shouldThrowWhenNoUserIdInSession() {
            ObjectId channelId = new ObjectId();
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
            accessor.setDestination("/topic/channel/" + channelId.toHexString());
            accessor.setSessionAttributes(null);
            accessor.setSubscriptionId("sub-1");
            Message<?> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            assertThatThrownBy(() -> interceptor.preSend(msg, messageChannel))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no userId in WebSocket session");
        }
    }

    // ────────────────────────────────────────
    // thread subscription validation
    // ────────────────────────────────────────

    @Nested
    class ThreadSubscription {

        @Test
        void shouldAllowMemberToSubscribeToThread() {
            ObjectId threadId = new ObjectId();
            ObjectId channelId = new ObjectId();
            ChatThread thread = ChatThread.builder()
                    .id(threadId)
                    .channelId(channelId)
                    .build();
            Channel channel = Channel.builder()
                    .id(channelId)
                    .type(ChannelType.GROUP)
                    .members(List.of(10))
                    .build();
            when(threadRepository.findActiveById(threadId)).thenReturn(Optional.of(thread));
            when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));

            Message<?> msg = buildSubscribeMessage("/topic/thread/" + threadId.toHexString(), 10);
            Message<?> result = interceptor.preSend(msg, messageChannel);

            assertThat(result).isNotNull();
        }

        @Test
        void shouldAllowMemberToSubscribeToThreadTyping() {
            ObjectId threadId = new ObjectId();
            ObjectId channelId = new ObjectId();
            ChatThread thread = ChatThread.builder()
                    .id(threadId)
                    .channelId(channelId)
                    .build();
            Channel channel = Channel.builder()
                    .id(channelId)
                    .type(ChannelType.GROUP)
                    .members(List.of(10))
                    .build();
            when(threadRepository.findActiveById(threadId)).thenReturn(Optional.of(thread));
            when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));

            Message<?> msg = buildSubscribeMessage("/topic/thread/" + threadId.toHexString() + "/typing", 10);
            Message<?> result = interceptor.preSend(msg, messageChannel);

            assertThat(result).isNotNull();
        }

        @Test
        void shouldRejectIfThreadNotFound() {
            ObjectId threadId = new ObjectId();
            when(threadRepository.findActiveById(threadId)).thenReturn(Optional.empty());

            Message<?> msg = buildSubscribeMessage("/topic/thread/" + threadId.toHexString(), 10);

            assertThatThrownBy(() -> interceptor.preSend(msg, messageChannel))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("thread not found");
        }

        @Test
        void shouldRejectIfNotMemberOfParentChannel() {
            ObjectId threadId = new ObjectId();
            ObjectId channelId = new ObjectId();
            ChatThread thread = ChatThread.builder()
                    .id(threadId)
                    .channelId(channelId)
                    .build();
            Channel channel = Channel.builder()
                    .id(channelId)
                    .type(ChannelType.GROUP)
                    .members(List.of(20, 30))
                    .build();
            when(threadRepository.findActiveById(threadId)).thenReturn(Optional.of(thread));
            when(channelRepository.findById(channelId)).thenReturn(Optional.of(channel));

            Message<?> msg = buildSubscribeMessage("/topic/thread/" + threadId.toHexString(), 10);

            assertThatThrownBy(() -> interceptor.preSend(msg, messageChannel))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not a member of channel");
        }
    }
}
