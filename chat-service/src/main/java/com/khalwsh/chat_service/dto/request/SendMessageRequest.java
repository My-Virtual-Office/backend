package com.khalwsh.chat_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    @NotBlank(message = "content is required")
    private String content;

    // if set, message goes into this thread instead of the main channel feed
    private String threadId;

    // if set, this message is an inline reply to another message
    private String replyToId;

    private List<Integer> mentions;

    // optional client-generated UUID for idempotent sends
    private String clientMessageId;
}
