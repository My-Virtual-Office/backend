package com.khalwsh.chat_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// payload sent via STOMP to /app/chat/send
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StompSendMessage {

    private String channelId;
    private String content;

    // optional — if set, message goes inside a thread
    private String threadId;

    // optional — inline reply
    private String replyToId;

    private List<Integer> mentions;
    private String clientMessageId;
}
