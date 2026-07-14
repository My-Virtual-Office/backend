package com.virtualoffice.tasks.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * A task within a workspace (ClickUp-style). {@code taskNumber} is a per-workspace sequential
 * counter (see the service) that powers @task-number chat mentions and is unique per workspace.
 * {@code status} / {@code priority} are stored as Strings (validated against
 * {@link TaskStatus} / {@link TaskPriority}).
 */
@Entity
@Table(name = "task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Per-workspace sequential number; unique per (workspaceId, taskNumber) at the DB level.
    @Column(nullable = false)
    private Long taskNumber;

    @Column(nullable = false)
    private Long workspaceId;

    // The Team Space this task belongs to (tasks are scoped per space).
    @Column(nullable = false)
    private Long spaceId;

    @Column(nullable = false)
    private String title;

    private String description;

    // TODO | IN_PROGRESS | COMPLETE
    @Column(nullable = false)
    private String status;

    // LOW | NORMAL | HIGH
    @Column(nullable = false)
    private String priority;

    private Instant dueDate;

    private Long assigneeUserId;

    // The caller who created the task (from the trusted X-User-Id header).
    @Column(nullable = false)
    private Long createdBy;

    // Minutes before dueDate to send a "due soon" reminder (null = no reminder).
    private Integer reminderMinutes;

    // Set once the reminder has been dispatched, so it fires at most once (cleared on due/reminder change).
    private Instant reminderSentAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
