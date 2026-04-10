package com.khalwsh.chat_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkReadRequest {

    @NotNull(message = "lastReadMessageId is required")
    private String lastReadMessageId;
}
