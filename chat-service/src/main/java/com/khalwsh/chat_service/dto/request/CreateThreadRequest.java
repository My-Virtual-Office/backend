package com.khalwsh.chat_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateThreadRequest {

    @NotNull(message = "rootMessageId is required")
    private String rootMessageId;

    @NotBlank(message = "thread name is required")
    private String name;
}
