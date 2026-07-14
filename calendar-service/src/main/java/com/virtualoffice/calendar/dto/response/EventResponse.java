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
package com.virtualoffice.calendar.dto.response;

import com.virtualoffice.calendar.model.CalendarEvent;

import java.time.Instant;

public record EventResponse(
        Long id,
        Long userId,
        Long workspaceId,
        String title,
        String description,
        Instant startTime,
        Instant endTime,
        boolean busy,
        Instant createdAt,
        Instant updatedAt) {

    public static EventResponse from(CalendarEvent e) {
        return new EventResponse(
                e.getId(), e.getUserId(), e.getWorkspaceId(), e.getTitle(), e.getDescription(),
                e.getStartTime(), e.getEndTime(), e.isBusy(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
