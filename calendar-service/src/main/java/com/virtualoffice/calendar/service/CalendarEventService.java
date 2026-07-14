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
package com.virtualoffice.calendar.service;

import com.virtualoffice.calendar.dto.request.CreateEventRequest;
import com.virtualoffice.calendar.dto.request.UpdateEventRequest;
import com.virtualoffice.calendar.dto.response.EventResponse;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Owner-scoped CRUD for calendar events; every operation acts on behalf of {@code userId}. */
public interface CalendarEventService {

    EventResponse create(Long userId, String email, CreateEventRequest request);

    List<EventResponse> list(Long userId, Long workspaceId, Instant from, Instant to);

    EventResponse get(Long userId, Long id);

    EventResponse update(Long userId, Long id, UpdateEventRequest request);

    void delete(Long userId, Long id);

    /** The caller's currently-active event in the workspace (start &le; now &lt; end), if any. */
    Optional<EventResponse> current(Long userId, Long workspaceId);
}
