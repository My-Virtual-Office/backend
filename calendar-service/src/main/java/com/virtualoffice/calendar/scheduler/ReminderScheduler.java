package com.virtualoffice.calendar.scheduler;

import com.virtualoffice.calendar.messaging.NotificationPublisher;
import com.virtualoffice.calendar.messaging.NotificationType;
import com.virtualoffice.calendar.model.CalendarEvent;
import com.virtualoffice.calendar.repository.CalendarEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Fires MEETING_REMINDER (email + in-app) to the owner and every attendee, once, before start. */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReminderScheduler {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("EEE, MMM d 'at' h:mm a 'UTC'").withZone(ZoneOffset.UTC);

    private final CalendarEventRepository repository;
    private final NotificationPublisher publisher;

    @Value("${app.url:http://localhost:3001}")
    private String appUrl;

    @Scheduled(fixedDelayString = "${calendar.reminder.interval-ms:60000}")
    @Transactional
    public void dispatchDueReminders() {
        Instant now = Instant.now();
        List<CalendarEvent> pending = repository
                .findByReminderSentAtIsNullAndReminderMinutesIsNotNullAndStartTimeAfter(now);

        for (CalendarEvent e : pending) {
            Instant remindAt = e.getStartTime().minus(e.getReminderMinutes(), ChronoUnit.MINUTES);
            if (now.isBefore(remindAt)) continue; // not due yet

            long minutesUntil = Math.max(0, ChronoUnit.MINUTES.between(now, e.getStartTime()));
            String startStr = FMT.format(e.getStartTime());

            // recipients = owner + attendees (userId -> email)
            Map<Long, String> recipients = new HashMap<>();
            recipients.put(e.getUserId(), e.getCreatorEmail());
            if (e.getAttendees() != null) recipients.putAll(e.getAttendees());

            for (Map.Entry<Long, String> r : recipients.entrySet()) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("userId", r.getKey());
                if (r.getValue() != null && !r.getValue().isBlank()) payload.put("email", r.getValue());
                payload.put("title", e.getTitle());
                payload.put("body", "\"" + e.getTitle() + "\" starts in " + minutesUntil + " min");
                payload.put("startTime", startStr);
                payload.put("minutesUntil", String.valueOf(minutesUntil));
                payload.put("appUrl", appUrl);
                publisher.publish(NotificationType.MEETING_REMINDER, payload);
            }

            e.setReminderSentAt(now);
            repository.save(e);
            log.info("Reminder dispatched for event {} to {} recipient(s)", e.getId(), recipients.size());
        }
    }
}
