package com.virtualoffice.chat_service.scheduler;

import com.virtualoffice.chat_service.dto.request.SendMessageRequest;
import com.virtualoffice.chat_service.dto.response.MessageResponse;
import com.virtualoffice.chat_service.dto.response.WebSocketEvent;
import com.virtualoffice.chat_service.model.ScheduledMessage;
import com.virtualoffice.chat_service.repository.ScheduledMessageRepository;
import com.virtualoffice.chat_service.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/** Delivers scheduled messages once their time arrives (broadcasts like a live send). */
@Component
@RequiredArgsConstructor
public class ScheduledMessageDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ScheduledMessageDispatcher.class);

    private final ScheduledMessageRepository repository;
    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedDelayString = "${chat.scheduled.dispatch-interval-ms:20000}")
    public void dispatchDue() {
        List<ScheduledMessage> due = repository.findDue(Instant.now());
        for (ScheduledMessage sm : due) {
            try {
                SendMessageRequest req = new SendMessageRequest();
                req.setContent(sm.getContent());
                // Stable id makes redelivery (e.g. after a crash before marking sent) idempotent.
                req.setClientMessageId("sched-" + sm.getId().toHexString());

                MessageResponse resp = messageService.sendMessage(
                        sm.getChannelId().toHexString(), req, sm.getSenderId(), sm.getSenderRole());

                messagingTemplate.convertAndSend(
                        "/topic/channel/" + resp.getChannelId(),
                        WebSocketEvent.of(WebSocketEvent.NEW_MESSAGE, resp));
            } catch (Exception e) {
                log.warn("failed to dispatch scheduled message {}: {}", sm.getId(), e.getMessage());
            } finally {
                // mark delivered regardless so a poison message can't loop forever
                sm.setSent(true);
                repository.save(sm);
            }
        }
    }
}
