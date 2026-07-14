package com.virtualoffice.tasks.dto.response;

import com.virtualoffice.tasks.model.Task;

import java.time.Instant;

/** Wire shape returned for every task endpoint. Field names/order are part of the frontend contract. */
public record TaskResponse(
        Long id,
        Long taskNumber,
        Long workspaceId,
        Long spaceId,
        String title,
        String description,
        String status,
        String priority,
        Instant dueDate,
        Long assigneeUserId,
        Long createdBy,
        Integer reminderMinutes,
        Instant createdAt,
        Instant updatedAt) {

    public static TaskResponse from(Task t) {
        return new TaskResponse(
                t.getId(),
                t.getTaskNumber(),
                t.getWorkspaceId(),
                t.getSpaceId(),
                t.getTitle(),
                t.getDescription(),
                t.getStatus(),
                t.getPriority(),
                t.getDueDate(),
                t.getAssigneeUserId(),
                t.getCreatedBy(),
                t.getReminderMinutes(),
                t.getCreatedAt(),
                t.getUpdatedAt());
    }
}
