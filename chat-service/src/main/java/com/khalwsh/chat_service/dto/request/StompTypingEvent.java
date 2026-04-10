package com.khalwsh.chat_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// payload sent via STOMP to /app/chat/typing
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StompTypingEvent {

    private String channelId;

    // optional — typing inside a thread
    private String threadId;

    private boolean typing;
}
