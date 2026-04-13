package com.khalwsh.chat_service.controller;

import com.khalwsh.chat_service.dto.request.EditMessageRequest;
import com.khalwsh.chat_service.dto.request.SendMessageRequest;
import com.khalwsh.chat_service.dto.response.MessageResponse;
import com.khalwsh.chat_service.dto.response.PaginatedResponse;
import com.khalwsh.chat_service.service.MessageService;
import com.khalwsh.chat_service.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/channels/{id}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable String id,
            @Valid @RequestBody SendMessageRequest request,
            HttpServletRequest httpRequest) {

        UserContext.UserInfo user = UserContext.fromRequest(httpRequest);

        if(!user.validate()){
            return ResponseEntity.badRequest().build();
        }

        MessageResponse response = messageService.sendMessage(id, request, user.getUserId(), user.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/channels/{id}/messages")
    public ResponseEntity<?> getMessages(
            @PathVariable String id,
            @RequestParam(required = false) String before,
            @RequestParam(required = false) String after,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit) {

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

        if(!user.validate()){
            return ResponseEntity.badRequest().build();
        }

        MessageResponse response = messageService.editMessage(id, request.getContent(), user.getUserId(), user.getRole());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable String id,
            HttpServletRequest httpRequest) {

        UserContext.UserInfo user = UserContext.fromRequest(httpRequest);

        if(!user.validate()){
            return ResponseEntity.badRequest().build();
        }

        messageService.deleteMessage(id, user.getUserId(), user.getRole());
        return ResponseEntity.ok().build();
    }
}
