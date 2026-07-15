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
package com.virtualoffice.calendar.service.impl;

import com.virtualoffice.calendar.dto.request.CreateEventRequest;
import com.virtualoffice.calendar.dto.request.UpdateEventRequest;
import com.virtualoffice.calendar.dto.response.EventResponse;
import com.virtualoffice.calendar.exception.ResourceNotFoundException;
import com.virtualoffice.calendar.model.CalendarEvent;
import com.virtualoffice.calendar.repository.CalendarEventRepository;
import com.virtualoffice.calendar.service.CalendarEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CalendarEventServiceImpl implements CalendarEventService {

    private final CalendarEventRepository repository;
    private final com.virtualoffice.calendar.client.WorkspaceStatusClient workspaceClient;

    public CalendarEventServiceImpl(CalendarEventRepository repository,
                                    com.virtualoffice.calendar.client.WorkspaceStatusClient workspaceClient) {
        this.repository = repository;
        this.workspaceClient = workspaceClient;
    }

    @Override
    public EventResponse create(Long userId, String email, CreateEventRequest request) {
        requireValidWindow(request.startTime(), request.endTime());
        CalendarEvent event = CalendarEvent.builder()
                .userId(userId)
                .workspaceId(request.workspaceId())
                .title(request.title())
                .description(request.description())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .busy(request.busy() == null || request.busy())
                .color(request.color())
                .meetingLink(request.meetingLink())
                .creatorEmail(email)
                .reminderMinutes(request.reminderMinutes())
                .attendees(toAttendeeMap(request.attendees()))
                .build();
        return EventResponse.from(repository.save(event));
    }

    private static java.util.Map<Long, String> toAttendeeMap(
            java.util.List<com.virtualoffice.calendar.dto.request.AttendeeInput> attendees) {
        java.util.Map<Long, String> map = new java.util.HashMap<>();
        if (attendees != null) {
            for (var a : attendees) {
                if (a != null && a.userId() != null) map.put(a.userId(), a.email());
            }
        }
        return map;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> list(Long userId, Long workspaceId, Instant from, Instant to) {
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("'from' must be before 'to'");
        }
        // Shared team calendar: return every event in the workspace overlapping the window
        // (an event is in range if it starts before `to` and ends after `from`).
        return repository
                .findByWorkspaceIdAndStartTimeLessThanAndEndTimeGreaterThanOrderByStartTimeAsc(
                        workspaceId, to, from)
                .stream().map(EventResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse get(Long userId, Long id) {
        return EventResponse.from(requireOwn(userId, id));
    }

    @Override
    public EventResponse update(Long userId, Long id, UpdateEventRequest request) {
        requireValidWindow(request.startTime(), request.endTime());
        CalendarEvent event = requireEditable(userId, id);
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        event.setBusy(request.busy() == null || request.busy());
        event.setColor(request.color());
        event.setMeetingLink(request.meetingLink());
        event.setReminderMinutes(request.reminderMinutes());
        event.setReminderSentAt(null); // window/reminder changed → allow it to fire again
        event.setAttendees(toAttendeeMap(request.attendees()));
        return EventResponse.from(repository.save(event));
    }

    @Override
    public void delete(Long userId, Long id) {
        repository.delete(requireEditable(userId, id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EventResponse> current(Long userId, Long workspaceId) {
        Instant now = Instant.now();
        return repository
                .findByUserIdAndWorkspaceIdAndStartTimeLessThanEqualAndEndTimeGreaterThanOrderByStartTimeDesc(
                        userId, workspaceId, now, now)
                .stream().findFirst().map(EventResponse::from);
    }

    // --- helpers ---

    private CalendarEvent requireOwn(Long userId, Long id) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("event not found: " + id));
    }

    // Editable by the event's author OR a workspace admin/owner (server-to-server role check).
    private CalendarEvent requireEditable(Long callerId, Long id) {
        CalendarEvent event = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("event not found: " + id));
        if (event.getUserId().equals(callerId)) return event;
        if (workspaceClient.isAdmin(event.getWorkspaceId(), callerId)) return event;
        throw new com.virtualoffice.calendar.exception.ForbiddenException(
                "only the event owner or a workspace admin can modify this event");
    }

    private void requireValidWindow(Instant start, Instant end) {
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
    }
}
