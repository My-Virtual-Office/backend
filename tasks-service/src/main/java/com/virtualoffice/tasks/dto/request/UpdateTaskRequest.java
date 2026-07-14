package com.virtualoffice.tasks.dto.request;

import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Partial update (PATCH): every field is optional. Only fields present with a non-null value are
 * applied (board drag-to-move sends just {@code status}; reassign sends just {@code assigneeUserId}).
 */
public record UpdateTaskRequest(
        @Size(max = 500) String title,
        @Size(max = 8000) String description,
        String status,
        String priority,
        Instant dueDate,
        Long assigneeUserId,
        Integer reminderMinutes) {
}
