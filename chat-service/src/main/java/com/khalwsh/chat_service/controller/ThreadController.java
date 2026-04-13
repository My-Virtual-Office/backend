package com.khalwsh.chat_service.controller;

import com.khalwsh.chat_service.dto.request.CreateThreadRequest;
import com.khalwsh.chat_service.dto.response.MessageResponse;
import com.khalwsh.chat_service.dto.response.PaginatedResponse;
import com.khalwsh.chat_service.dto.response.ThreadResponse;
import com.khalwsh.chat_service.dto.response.WebSocketEvent;
import com.khalwsh.chat_service.service.ChannelService;
import com.khalwsh.chat_service.service.MessageService;
import com.khalwsh.chat_service.service.ThreadService;
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
public class ThreadController {

    private final ThreadService threadService;
    private final MessageService messageService;
    private final ChannelService channelService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/channels/{id}/threads")
    public ResponseEntity<ThreadResponse> createThread(
            @PathVariable String id,
            @Valid @RequestBody CreateThreadRequest request,
            HttpServletRequest httpRequest) {

        UserContext.UserInfo user = UserContext.fromRequest(httpRequest);

        ThreadResponse response = threadService.createThread(id, request, user.getUserId(), user.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/channels/{id}/threads")
    public ResponseEntity<PaginatedResponse<ThreadResponse>> getChannelThreads(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            HttpServletRequest httpRequest) {

        UserContext.UserInfo user = UserContext.fromRequest(httpRequest);

        // must be a channel member to list its threads
        if (!channelService.isMember(id, user.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not a member of this channel");
        }

        PaginatedResponse<ThreadResponse> response = threadService.getChannelThreads(id, page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/threads/{threadId}")
    public ResponseEntity<ThreadResponse> getThread(
            @PathVariable String threadId,
            HttpServletRequest httpRequest) {

        UserContext.UserInfo user = UserContext.fromRequest(httpRequest);

        // check membership via the thread's parent channel
        ThreadResponse thread = threadService.getThread(threadId);
        if (!channelService.isMember(thread.getChannelId(), user.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not a member of the parent channel");
        }

        return ResponseEntity.ok(thread);
    }

    @DeleteMapping("/threads/{threadId}")
    public ResponseEntity<Void> deleteThread(
            @PathVariable String threadId,
            HttpServletRequest httpRequest) {

        UserContext.UserInfo user = UserContext.fromRequest(httpRequest);

        threadService.deleteThread(threadId, user.getUserId(), user.getRole());

        // broadcast thread deleted to channel subscribers
        WebSocketEvent<Map<String, String>> event = WebSocketEvent.of(
                WebSocketEvent.THREAD_DELETED, Map.of("threadId", threadId));
        messagingTemplate.convertAndSend("/topic/thread/" + threadId, event);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/threads/{threadId}/messages")
    public ResponseEntity<?> getThreadMessages(
            @PathVariable String threadId,
            @RequestParam(required = false) String before,
            @RequestParam(required = false) String after,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit,
            HttpServletRequest httpRequest) {

        UserContext.UserInfo user = UserContext.fromRequest(httpRequest);

        // check membership via the thread's parent channel
        ThreadResponse thread = threadService.getThread(threadId);
        if (!channelService.isMember(thread.getChannelId(), user.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not a member of the parent channel");
        }

        if (before != null) {
            List<MessageResponse> messages = messageService.getThreadMessagesBefore(threadId, before, limit);
            return ResponseEntity.ok(messages);
        }

        if (after != null) {
            List<MessageResponse> messages = messageService.getThreadMessagesAfter(threadId, after, limit);
            return ResponseEntity.ok(messages);
        }

        PaginatedResponse<MessageResponse> response = messageService.getThreadMessages(threadId, page, limit);
        return ResponseEntity.ok(response);
    }
}
