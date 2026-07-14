package com.virtualoffice.tasks.messaging;

// Serialized by name over RabbitMQ; each value must match notifications-service's enum.
public enum NotificationType {
    TASK_ASSIGNED,
    TASK_REMINDER
}
