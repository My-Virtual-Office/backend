package com.virtualoffice.tasks.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Create a task. {@code createdBy} is taken from the caller (not this body). {@code status}
 * defaults to TODO and {@code priority} to NORMAL when omitted; both are validated by the service.
 */
public record CreateTaskRequest(
        @NotNull Long workspaceId,
        // The Team Space to create the task in. Null falls back to the workspace's default space.
        Long spaceId,
        @NotBlank @Size(max = 500) String title,
        @Size(max = 8000) String description,
        String status,
        String priority,
        Instant dueDate,
        Long assigneeUserId,
        Integer reminderMinutes) {
}
