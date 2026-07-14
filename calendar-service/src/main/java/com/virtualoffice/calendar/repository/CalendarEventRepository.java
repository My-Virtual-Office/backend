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
package com.virtualoffice.calendar.repository;

import com.virtualoffice.calendar.model.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    // Owner-scoped fetch — the guard for read/update/delete of a single event.
    Optional<CalendarEvent> findByIdAndUserId(Long id, Long userId);

    // A caller's events overlapping the window [from, to): start < to AND end > from.
    List<CalendarEvent> findByUserIdAndWorkspaceIdAndStartTimeLessThanAndEndTimeGreaterThanOrderByStartTimeAsc(
            Long userId, Long workspaceId, Instant to, Instant from);

    // A caller's currently-active events (start <= now < end); newest-starting first so callers can take(0).
    List<CalendarEvent> findByUserIdAndWorkspaceIdAndStartTimeLessThanEqualAndEndTimeGreaterThanOrderByStartTimeDesc(
            Long userId, Long workspaceId, Instant nowForStart, Instant nowForEnd);

    // Auto-status scan: every busy event whose window contains "now", across all users/workspaces.
    List<CalendarEvent> findByBusyTrueAndStartTimeLessThanEqualAndEndTimeGreaterThan(
            Instant nowForStart, Instant nowForEnd);

    // Reminder scan: future events with a reminder set that hasn't fired yet.
    List<CalendarEvent> findByReminderSentAtIsNullAndReminderMinutesIsNotNullAndStartTimeAfter(Instant now);
}
