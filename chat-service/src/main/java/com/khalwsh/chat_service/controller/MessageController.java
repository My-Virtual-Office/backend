package com.khalwsh.chat_service.controller;

import com.khalwsh.chat_service.dto.request.EditMessageRequest;
import com.khalwsh.chat_service.dto.request.SendMessageRequest;
import com.khalwsh.chat_service.dto.response.MessageResponse;
import com.khalwsh.chat_service.dto.response.PaginatedResponse;
import com.khalwsh.chat_service.dto.response.WebSocketEvent;
import com.khalwsh.chat_service.service.ChannelService;
import com.khalwsh.chat_service.service.MessageService;
import com.khalwsh.chat_service.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final ChannelService channelService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/channels/{id}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable String id,
            @Valid @RequestBody SendMessageRequest request,
            HttpServletRequest httpRequest) {

        UserContext.UserInfo user = UserContext.fromRequest(httpRequest);

        MessageResponse response = messageService.sendMessage(id, request, user.getUserId(), user.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/channels/{id}/messages")
    public ResponseEntity<?> getMessages(
            @PathVariable String id,
            @RequestParam(required = false) String before,
            @RequestParam(required = false) String after,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit,
            HttpServletRequest httpRequest) {

        UserContext.UserInfo user = UserContext.fromRequest(httpRequest);

        // must be a member to read messages
        if (!channelService.isMember(id, user.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not a member of this channel");
        }

        // cursor-based takes priority over page-based
        if (before != null) {
            List<MessageResponse> messages = messageService.getChannelMessagesBefore(id, before, limit);
            return ResponseEntity.ok(messages);
        }

        if (after != null) {
            List<MessageResponse> messages = messageService.getChannelMessagesAfter(id, after, limit);
            return ResponseEntity.ok(messages);
        }

        // fallback to page-based pagination
        PaginatedResponse<MessageResponse> response = messageService.getChannelMessages(id, page, limit);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/messages/{id}")
    public ResponseEntity<MessageResponse> editMessage(
            @PathVariable String id,
            @Valid @RequestBody EditMessageRequest request,
            HttpServletRequest httpRequest) {

        UserContext.UserInfo user = UserContext.fromRequest(httpRequest);
        MessageResponse response = messageService.editMessage(id, request.getContent(), user.getUserId(), user.getRole());

        // broadcast edit to subscribers
        WebSocketEvent<MessageResponse> event = WebSocketEvent.of(WebSocketEvent.EDIT_MESSAGE, response);
        String topic = response.getThreadId() != null
                ? "/topic/thread/" + response.getThreadId()
                : "/topic/channel/" + response.getChannelId();
        messagingTemplate.convertAndSend(topic, event);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable String id,
            HttpServletRequest httpRequest) {

        UserContext.UserInfo user = UserContext.fromRequest(httpRequest);
        MessageResponse deleted = messageService.deleteMessage(id, user.getUserId(), user.getRole());

        // broadcast delete to subscribers
        if (deleted != null) {
            WebSocketEvent<Map<String, String>> event = WebSocketEvent.of(
                    WebSocketEvent.DELETE_MESSAGE, Map.of("messageId", id));
            String topic = deleted.getThreadId() != null
                    ? "/topic/thread/" + deleted.getThreadId()
                    : "/topic/channel/" + deleted.getChannelId();
            messagingTemplate.convertAndSend(topic, event);
        }

        return ResponseEntity.ok().build();
    }
}
