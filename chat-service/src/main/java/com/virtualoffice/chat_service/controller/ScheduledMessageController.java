package com.virtualoffice.chat_service.controller;

import com.virtualoffice.chat_service.dto.request.CreateScheduledMessageRequest;
import com.virtualoffice.chat_service.dto.response.ScheduledMessageResponse;
import com.virtualoffice.chat_service.model.ScheduledMessage;
import com.virtualoffice.chat_service.repository.ScheduledMessageRepository;
import com.virtualoffice.chat_service.service.ChannelService;
import com.virtualoffice.chat_service.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ScheduledMessageController {

    private final ScheduledMessageRepository repository;
    private final ChannelService channelService;

    /** Queue a message to be delivered to a channel at a future time. */
    @PostMapping("/channels/{id}/scheduled")
    public ResponseEntity<ScheduledMessageResponse> schedule(
            @PathVariable String id,
            @Valid @RequestBody CreateScheduledMessageRequest request,
            HttpServletRequest http) {

        UserContext.UserInfo user = UserContext.fromRequest(http);
        if (!channelService.isMember(id, user.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not a member of this channel");
        }
        if (request.getScheduledAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scheduledAt must be in the future");
        }

        ScheduledMessage sm = repository.save(ScheduledMessage.builder()
                .channelId(new ObjectId(id))
                .senderId(user.getUserId())
                .senderRole(user.getRole())
                .content(request.getContent())
                .scheduledAt(request.getScheduledAt())
                .sent(false)
                .createdAt(Instant.now())
                .build());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(sm));
    }

    /** My pending scheduled messages in a channel. */
    @GetMapping("/channels/{id}/scheduled")
    public List<ScheduledMessageResponse> list(@PathVariable String id, HttpServletRequest http) {
        UserContext.UserInfo user = UserContext.fromRequest(http);
        if (!channelService.isMember(id, user.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not a member of this channel");
        }
        return repository.findPendingForSender(new ObjectId(id), user.getUserId())
                .stream().map(this::toResponse).toList();
    }

    /** Cancel one of my pending scheduled messages. */
    @DeleteMapping("/scheduled/{scheduledId}")
    public ResponseEntity<Void> cancel(@PathVariable String scheduledId, HttpServletRequest http) {
        UserContext.UserInfo user = UserContext.fromRequest(http);
        ScheduledMessage sm = repository.findById(new ObjectId(scheduledId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "not found"));
        if (!sm.getSenderId().equals(user.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not your scheduled message");
        }
        repository.deleteById(sm.getId());
        return ResponseEntity.noContent().build();
    }

    private ScheduledMessageResponse toResponse(ScheduledMessage sm) {
        return ScheduledMessageResponse.builder()
                .id(sm.getId().toHexString())
                .channelId(sm.getChannelId().toHexString())
                .content(sm.getContent())
                .scheduledAt(sm.getScheduledAt())
                .build();
    }
}
