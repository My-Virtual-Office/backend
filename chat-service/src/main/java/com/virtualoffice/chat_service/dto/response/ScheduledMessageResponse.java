package com.virtualoffice.chat_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledMessageResponse {

    private String id;
    private String channelId;
    private String content;
    private Instant scheduledAt;
}
