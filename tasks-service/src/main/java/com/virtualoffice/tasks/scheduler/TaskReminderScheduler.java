package com.virtualoffice.tasks.scheduler;

import com.virtualoffice.tasks.messaging.NotificationPublisher;
import com.virtualoffice.tasks.messaging.NotificationType;
import com.virtualoffice.tasks.model.Task;
import com.virtualoffice.tasks.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fires TASK_REMINDER (in-app only) once, when now is within reminderMinutes of a task's dueDate.
 * Recipient is the assignee, or the creator if the task is unassigned.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TaskReminderScheduler {

    private final TaskRepository repository;
    private final NotificationPublisher publisher;

    @Scheduled(fixedDelayString = "${tasks.reminder.interval-ms:60000}")
    @Transactional
    public void dispatchDueReminders() {
        Instant now = Instant.now();
        List<Task> pending = repository
                .findByReminderSentAtIsNullAndReminderMinutesIsNotNullAndDueDateIsNotNullAndDueDateAfter(now);

        for (Task t : pending) {
            Instant remindAt = t.getDueDate().minus(t.getReminderMinutes(), ChronoUnit.MINUTES);
            if (now.isBefore(remindAt)) continue; // not due yet

            long minutesUntil = Math.max(0, ChronoUnit.MINUTES.between(now, t.getDueDate()));
            Long recipient = t.getAssigneeUserId() != null ? t.getAssigneeUserId() : t.getCreatedBy();

            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", recipient);
            payload.put("title", "Task due soon");
            payload.put("body", "\"" + t.getTitle() + "\" is due in " + minutesUntil + " min");
            publisher.publish(NotificationType.TASK_REMINDER, payload);

            t.setReminderSentAt(now);
            repository.save(t);
            log.info("Task reminder dispatched for task {} to user {}", t.getId(), recipient);
        }
    }
}
