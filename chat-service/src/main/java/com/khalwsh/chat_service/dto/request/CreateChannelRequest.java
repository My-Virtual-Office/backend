package com.khalwsh.chat_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChannelRequest {

    @NotBlank(message = "channel name is required")
    private String name;

    @NotNull(message = "workspaceId is required")
    private Integer workspaceId;

    @NotEmpty(message = "at least one member is required")
    private List<Integer> members;
}
