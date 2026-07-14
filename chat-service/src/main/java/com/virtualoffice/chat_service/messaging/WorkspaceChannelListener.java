/*
 * Copyright (c) 2025 My Virtual Office
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 */
package com.virtualoffice.chat_service.messaging;

import com.virtualoffice.chat_service.dto.mapper.DtoMapper;
import com.virtualoffice.chat_service.dto.response.WebSocketEvent;
import com.virtualoffice.chat_service.model.Channel;
import com.virtualoffice.chat_service.model.ChannelType;
import com.virtualoffice.chat_service.model.Message;
import com.virtualoffice.chat_service.model.MessageType;
import com.virtualoffice.chat_service.repository.ChannelRepository;
import com.virtualoffice.chat_service.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Provisions and syncs the canonical per-workspace chat channel from workspace-service events
 * (INTEGRATION.md §5.1). Sibling of {@link RoomChannelListener}; idempotent so redeliveries and
 * out-of-order events are safe. The canonical channel is keyed by {@code workspaceId} (workspace
 * -service does not know chat-side ids), resolved via {@link ChannelRepository#findCanonicalByWorkspaceId}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkspaceChannelListener {

    private static final String GENERAL_CHANNEL = "general";
    private static final String JOIN_CHANNEL = "join";

    private final ChannelRepository channelRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = "${workspace.channel.queue}")
    public void handle(WorkspaceChannelEvent event) {
        if (event.getType() == null || event.getWorkspaceId() == null) {
            log.warn("dropping malformed workspace-channel event: {}", event);
            return;
        }
        switch (event.getType()) {
            case WORKSPACE_CHANNEL_CREATE -> createChannel(event);
            case WORKSPACE_CHANNEL_ADD_MEMBER -> addMember(event);
            case WORKSPACE_CHANNEL_REMOVE_MEMBER -> removeMember(event);
        }
    }

    private void createChannel(WorkspaceChannelEvent event) {
        if (channelRepository.findCanonicalByWorkspaceId(event.getWorkspaceId()).isPresent()) {
            return; // already provisioned — idempotent
        }

        List<Integer> members = event.getMembers() != null
                ? new ArrayList<>(event.getMembers())
                : new ArrayList<>();
        Instant now = Instant.now();

        // #general — the canonical per-workspace channel everyone lands in.
        channelRepository.save(Channel.builder()
                .name(GENERAL_CHANNEL)
                .type(ChannelType.GROUP)
                .workspaceId(event.getWorkspaceId())
                .members(new ArrayList<>(members))
                .canonical(true)
                .createdBy(event.getUserId())
                .createdAt(now)
                .updatedAt(now)
                .build());

        // #join — a system channel that announces when members join.
        Channel join = channelRepository.save(Channel.builder()
                .name(JOIN_CHANNEL)
                .type(ChannelType.GROUP)
                .workspaceId(event.getWorkspaceId())
                .members(new ArrayList<>(members))
                .canonical(false)
                .createdBy(event.getUserId())
                .createdAt(now)
                .updatedAt(now)
                .build());
        postSystem(join, "Workspace created 🎉 — new members will be announced here.");

        log.info("provisioned #general + #join for workspace {}", event.getWorkspaceId());
    }

    private void addMember(WorkspaceChannelEvent event) {
        if (event.getUserId() == null) {
            return;
        }
        Instant now = Instant.now();
        channelRepository.findCanonicalByWorkspaceId(event.getWorkspaceId())
                .ifPresentOrElse(
                        c -> channelRepository.addMember(c.getId(), event.getUserId(), now),
                        () -> log.warn("no #general for workspace {}; dropping ADD_MEMBER",
                                event.getWorkspaceId()));
        channelRepository.findByWorkspaceIdAndName(event.getWorkspaceId(), JOIN_CHANNEL)
                .ifPresent(join -> {
                    channelRepository.addMember(join.getId(), event.getUserId(), now);
                    postSystem(join, "User " + event.getUserId() + " joined the workspace 👋");
                });
    }

    private void removeMember(WorkspaceChannelEvent event) {
        if (event.getUserId() == null) {
            return;
        }
        Instant now = Instant.now();
        channelRepository.findCanonicalByWorkspaceId(event.getWorkspaceId())
                .ifPresent(c -> channelRepository.removeMember(c.getId(), event.getUserId(), now));
        channelRepository.findByWorkspaceIdAndName(event.getWorkspaceId(), JOIN_CHANNEL)
                .ifPresent(j -> channelRepository.removeMember(j.getId(), event.getUserId(), now));
    }

    /** Persist a SYSTEM message in the channel and broadcast it live to subscribers. */
    private void postSystem(Channel channel, String content) {
        Instant now = Instant.now();
        Message saved = messageRepository.save(Message.builder()
                .channelId(channel.getId())
                .senderId(0)
                .senderRole("SYSTEM")
                .content(content)
                .type(MessageType.SYSTEM)
                .deleted(false)
                .createdAt(now)
                .updatedAt(now)
                .build());
        messagingTemplate.convertAndSend("/topic/channel/" + channel.getId(),
                WebSocketEvent.of(WebSocketEvent.NEW_MESSAGE, DtoMapper.toMessageResponse(saved)));
    }
}
