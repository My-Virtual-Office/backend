/*
 * Copyright (c) 2025 My Virtual Office
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 */
package com.virtualoffice.calendar.scheduler;

import com.virtualoffice.calendar.client.WorkspaceStatusClient;
import com.virtualoffice.calendar.model.CalendarEvent;
import com.virtualoffice.calendar.repository.CalendarEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Auto-status-from-calendar: while a member's own {@code busy} event is running, their Desk shows
 * "In a meeting"; when it ends, the status is reverted — but only if it still carries our marker,
 * so a status the member changed by hand during the meeting is never clobbered.
 *
 * Only the event OWNER is flipped (it's their "set me to In a meeting" checkbox); attendees keep
 * whatever status they chose.
 */
@Component
@Slf4j
public class AutoStatusScheduler {

    private static final String IN_MEETING = "DO_NOT_DISTURB";
    private static final String ACTIVE = "ACTIVE";

    private final CalendarEventRepository repository;
    private final WorkspaceStatusClient statusClient;
    private final boolean enabled;
    private final String markerText;
    private final String markerEmoji;

    public AutoStatusScheduler(CalendarEventRepository repository,
                               WorkspaceStatusClient statusClient,
                               @Value("${calendar.auto-status.enabled:true}") boolean enabled,
                               @Value("${calendar.auto-status.marker-text:In a meeting}") String markerText,
                               @Value("${calendar.auto-status.emoji:📅}") String markerEmoji) {
        this.repository = repository;
        this.statusClient = statusClient;
        this.enabled = enabled;
        this.markerText = markerText;
        this.markerEmoji = markerEmoji;
    }

    @Scheduled(fixedDelayString = "${calendar.auto-status.interval-ms:60000}")
    @Transactional
    public void sync() {
        if (!enabled) return;
        Instant now = Instant.now();
        applyForStartedMeetings(now);
        revertForEndedMeetings(now);
    }

    // Flip the owner to "In a meeting" for busy events that are now active and not yet applied.
    private void applyForStartedMeetings(Instant now) {
        List<CalendarEvent> starting = repository
                .findByBusyTrueAndStatusAppliedFalseAndStartTimeLessThanEqualAndEndTimeGreaterThan(now, now);
        for (CalendarEvent e : starting) {
            boolean ok = statusClient.setStatus(
                    e.getWorkspaceId(), e.getUserId(), IN_MEETING, markerEmoji, markerText);
            if (ok) {
                e.setStatusApplied(true);
                repository.save(e);
                log.info("auto-status: set user {} to '{}' for event {}", e.getUserId(), markerText, e.getId());
            }
            // transient failure → leave statusApplied=false, retry next tick
        }
    }

    // Revert owners whose flipped events have ended — but only if the status still carries our marker.
    private void revertForEndedMeetings(Instant now) {
        List<CalendarEvent> ended = repository
                .findByStatusAppliedTrueAndStatusClearedFalseAndEndTimeLessThanEqual(now);
        for (CalendarEvent e : ended) {
            Optional<String> current = statusClient.currentStatusText(e.getWorkspaceId(), e.getUserId());

            // Read failed → try again next tick (don't risk clobbering on a guess). But if the event
            // ended over an hour ago and we still can't read the desk (e.g. the owner left the
            // workspace), stop scanning it so stale rows don't accumulate.
            if (current.isEmpty()) {
                if (e.getEndTime().isBefore(now.minus(1, ChronoUnit.HOURS))) {
                    e.setStatusCleared(true);
                    repository.save(e);
                }
                continue;
            }

            String text = current.get();
            boolean stillOurMarker = markerText.equals(text);
            // A different, non-blank custom text means the member changed status by hand: leave it.
            boolean manuallyChanged = text != null && !text.isBlank() && !stillOurMarker;

            if (manuallyChanged) {
                e.setStatusCleared(true); // nothing to revert; don't keep scanning it
                repository.save(e);
                continue;
            }

            boolean ok = statusClient.setStatus(e.getWorkspaceId(), e.getUserId(), ACTIVE, "", "");
            if (ok) {
                e.setStatusCleared(true);
                repository.save(e);
                log.info("auto-status: reverted user {} to Active after event {}", e.getUserId(), e.getId());
            }
            // transient failure → retry next tick
        }
    }
}
