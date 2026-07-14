package com.virtualoffice.tasks.repository;

import com.virtualoffice.tasks.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // @task-number chat mentions: resolve a task by its per-workspace number.
    Optional<Task> findByWorkspaceIdAndTaskNumber(Long workspaceId, Long taskNumber);

    // "My tasks" in a workspace.
    List<Task> findByWorkspaceIdAndAssigneeUserIdOrderByTaskNumberDesc(Long workspaceId, Long assigneeUserId);

    // Next per-workspace task number: max(taskNumber) + 1 (0 for the first task in a workspace).
    @Query("select coalesce(max(t.taskNumber), 0) from Task t where t.workspaceId = :workspaceId")
    long findMaxTaskNumber(@Param("workspaceId") Long workspaceId);

    // List with optional assignee/status filters (both null-safe; the parameter type is inferred
    // from its typed column, so a null bind is fine here).
    @Query("""
            select t from Task t
            where t.workspaceId = :workspaceId
              and (:assigneeUserId is null or t.assigneeUserId = :assigneeUserId)
              and (:status is null or t.status = :status)
            order by t.taskNumber desc
            """)
    List<Task> filter(@Param("workspaceId") Long workspaceId,
                      @Param("assigneeUserId") Long assigneeUserId,
                      @Param("status") String status);

    // Same filters plus a required, non-null `q` matched against title OR description
    // (case-insensitive contains). Kept separate so a null `q` never reaches lower()/concat()
    // (Postgres would bind an untyped null as bytea and fail: "function lower(bytea) does not exist").
    @Query("""
            select t from Task t
            where t.workspaceId = :workspaceId
              and (:assigneeUserId is null or t.assigneeUserId = :assigneeUserId)
              and (:status is null or t.status = :status)
              and (lower(t.title) like lower(concat('%', :q, '%'))
                   or lower(coalesce(t.description, '')) like lower(concat('%', :q, '%')))
            order by t.taskNumber desc
            """)
    List<Task> search(@Param("workspaceId") Long workspaceId,
                      @Param("assigneeUserId") Long assigneeUserId,
                      @Param("status") String status,
                      @Param("q") String q);

    // Reminder scan: pending reminders whose due date is still in the future.
    List<Task> findByReminderSentAtIsNullAndReminderMinutesIsNotNullAndDueDateIsNotNullAndDueDateAfter(Instant now);
}
