package com.virtualoffice.chat_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class CreateScheduledMessageRequest {

    @NotBlank
    private String content;

    @NotNull
    private Instant scheduledAt;
}
