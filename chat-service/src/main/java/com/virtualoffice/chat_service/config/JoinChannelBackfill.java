package com.virtualoffice.chat_service.config;

import com.virtualoffice.chat_service.model.Channel;
import com.virtualoffice.chat_service.model.ChannelType;
import com.virtualoffice.chat_service.model.Message;
import com.virtualoffice.chat_service.model.MessageType;
import com.virtualoffice.chat_service.repository.ChannelRepository;
import com.virtualoffice.chat_service.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;

/**
 * One-time, idempotent backfill: workspaces created before #join existed have only a canonical
 * #general channel. On startup, create a #join channel (mirroring #general's members) for any
 * workspace missing one, and seed it with the welcome system message.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JoinChannelBackfill implements ApplicationRunner {

    private static final String JOIN_CHANNEL = "join";

    private final ChannelRepository channelRepository;
    private final MessageRepository messageRepository;

    @Override
    public void run(ApplicationArguments args) {
        int created = 0;
        for (Channel general : channelRepository.findByCanonicalTrue()) {
            Integer wsId = general.getWorkspaceId();
            if (wsId == null) {
                continue;
            }
            if (channelRepository.findByWorkspaceIdAndName(wsId, JOIN_CHANNEL).isPresent()) {
                continue; // already has #join
            }

            Instant now = Instant.now();
            Channel join = channelRepository.save(Channel.builder()
                    .name(JOIN_CHANNEL)
                    .type(ChannelType.GROUP)
                    .workspaceId(wsId)
                    .members(general.getMembers() != null
                            ? new ArrayList<>(general.getMembers()) : new ArrayList<>())
                    .canonical(false)
                    .createdBy(general.getCreatedBy())
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            messageRepository.save(Message.builder()
                    .channelId(join.getId())
                    .senderId(0)
                    .senderRole("SYSTEM")
                    .content("Welcome — new members will be announced here.")
                    .type(MessageType.SYSTEM)
                    .deleted(false)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            created++;
        }
        if (created > 0) {
            log.info("backfilled #join for {} existing workspace(s)", created);
        }
    }
}
