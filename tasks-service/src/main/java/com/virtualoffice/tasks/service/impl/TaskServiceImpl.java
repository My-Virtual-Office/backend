package com.virtualoffice.tasks.service.impl;

import com.virtualoffice.tasks.dto.request.CreateTaskRequest;
import com.virtualoffice.tasks.dto.request.UpdateTaskRequest;
import com.virtualoffice.tasks.dto.response.TaskResponse;
import com.virtualoffice.tasks.exception.ResourceNotFoundException;
import com.virtualoffice.tasks.messaging.NotificationPublisher;
import com.virtualoffice.tasks.messaging.NotificationType;
import com.virtualoffice.tasks.model.Task;
import com.virtualoffice.tasks.model.TaskPriority;
import com.virtualoffice.tasks.model.TaskSpace;
import com.virtualoffice.tasks.model.TaskStatus;
import com.virtualoffice.tasks.repository.TaskRepository;
import com.virtualoffice.tasks.service.SpaceService;
import com.virtualoffice.tasks.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository repository;
    private final NotificationPublisher publisher;
    private final SpaceService spaceService;

    @Override
    public TaskResponse create(Long callerId, String callerEmail, CreateTaskRequest req) {
        // Resolve (and access-check) the space; a null spaceId falls back to the workspace default.
        TaskSpace space = (req.spaceId() != null)
                ? spaceService.requireAccessibleSpace(callerId, req.spaceId())
                : spaceService.ensureDefault(callerId, req.workspaceId());
        long nextNumber = repository.findMaxTaskNumber(space.getWorkspaceId()) + 1;
        Task task = Task.builder()
                .taskNumber(nextNumber)
                .workspaceId(space.getWorkspaceId())
                .spaceId(space.getId())
                .title(req.title())
                .description(req.description())
                .status(normalizeStatus(req.status()))
                .priority(normalizePriority(req.priority()))
                .dueDate(req.dueDate())
                .assigneeUserId(req.assigneeUserId())
                .createdBy(callerId)
                .reminderMinutes(req.reminderMinutes())
                .build();
        Task saved = repository.save(task);
        if (saved.getAssigneeUserId() != null) {
            publishTaskAssigned(saved, callerEmail);
        }
        return TaskResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> list(Long callerId, Long workspaceId, Long spaceId,
                                   Long assigneeUserId, String status, String q) {
        // If a space is named, enforce membership; task rows are then filtered to it.
        if (spaceId != null) {
            spaceService.requireAccessibleSpace(callerId, spaceId);
        }
        String normStatus = (status == null || status.isBlank()) ? null : normalizeStatus(status);
        String normQ = (q == null || q.isBlank()) ? null : q.trim();
        List<Task> results = (normQ == null)
                ? repository.filter(workspaceId, spaceId, assigneeUserId, normStatus)
                : repository.search(workspaceId, spaceId, assigneeUserId, normStatus, normQ);
        return results.stream().map(TaskResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> mine(Long callerId, Long workspaceId) {
        return repository.findByWorkspaceIdAndAssigneeUserIdOrderByTaskNumberDesc(workspaceId, callerId)
                .stream().map(TaskResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return TaskResponse.from(require(id));
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getByNumber(Long workspaceId, Long taskNumber) {
        return TaskResponse.from(repository.findByWorkspaceIdAndTaskNumber(workspaceId, taskNumber)
                .orElseThrow(() -> new ResourceNotFoundException("task not found: #" + taskNumber)));
    }

    @Override
    public TaskResponse update(Long callerId, String callerEmail, Long id, UpdateTaskRequest req) {
        Task task = require(id);
        Long previousAssignee = task.getAssigneeUserId();

        if (req.title() != null) task.setTitle(req.title());
        if (req.description() != null) task.setDescription(req.description());
        if (req.status() != null) task.setStatus(normalizeStatus(req.status()));
        if (req.priority() != null) task.setPriority(normalizePriority(req.priority()));
        if (req.dueDate() != null) {
            task.setDueDate(req.dueDate());
            task.setReminderSentAt(null); // new due date → let the reminder fire again
        }
        if (req.assigneeUserId() != null) task.setAssigneeUserId(req.assigneeUserId());
        if (req.reminderMinutes() != null) {
            task.setReminderMinutes(req.reminderMinutes());
            task.setReminderSentAt(null); // reminder window changed → allow it to fire again
        }

        Task saved = repository.save(task);

        // Fire only when the assignee actually changes to a new, non-null user.
        if (req.assigneeUserId() != null && !req.assigneeUserId().equals(previousAssignee)) {
            publishTaskAssigned(saved, callerEmail);
        }
        return TaskResponse.from(saved);
    }

    @Override
    public void delete(Long id) {
        repository.delete(require(id));
    }

    // --- helpers ---

    private Task require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("task not found: " + id));
    }

    private void publishTaskAssigned(Task task, String callerEmail) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("assigneeUserId", task.getAssigneeUserId());
        // Best-effort actor name without an extra user-service round trip: the caller's email
        // (from X-User-Email) if present, else a neutral fallback.
        payload.put("assignedByName",
                (callerEmail != null && !callerEmail.isBlank()) ? callerEmail : "A teammate");
        payload.put("taskTitle", task.getTitle());
        payload.put("taskId", task.getId());
        payload.put("workspaceId", task.getWorkspaceId());
        publisher.publish(NotificationType.TASK_ASSIGNED, payload);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return TaskStatus.TODO.name();
        try {
            return TaskStatus.valueOf(status.trim().toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("status must be one of TODO, IN_PROGRESS, COMPLETE");
        }
    }

    private String normalizePriority(String priority) {
        if (priority == null || priority.isBlank()) return TaskPriority.NORMAL.name();
        try {
            return TaskPriority.valueOf(priority.trim().toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("priority must be one of LOW, NORMAL, HIGH");
        }
    }
}
