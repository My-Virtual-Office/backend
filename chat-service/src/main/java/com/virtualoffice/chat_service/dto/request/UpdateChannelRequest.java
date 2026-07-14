package com.virtualoffice.chat_service.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Partial update of a channel's settings (name, description, access). All fields optional;
 * a null field is left unchanged. Allowed for the channel creator or a moderator.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateChannelRequest {

    @Size(max = 100, message = "channel name must be 100 characters or less")
    private String name;

    @Size(max = 500)
    private String description;

    private String visibility; // "PUBLIC" | "PRIVATE"

    private List<Long> allowedTeamIds;

    private List<Integer> moderatorIds;

    private List<Integer> members;
}
