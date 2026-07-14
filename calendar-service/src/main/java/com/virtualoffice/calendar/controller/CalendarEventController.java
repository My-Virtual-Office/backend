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
package com.virtualoffice.calendar.controller;

import com.virtualoffice.calendar.dto.request.CreateEventRequest;
import com.virtualoffice.calendar.dto.request.UpdateEventRequest;
import com.virtualoffice.calendar.dto.response.EventResponse;
import com.virtualoffice.calendar.service.CalendarEventService;
import com.virtualoffice.calendar.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * Owner-scoped calendar CRUD. The caller's identity comes from the gateway-injected X-User-Id
 * (see {@link UserContext}); every event belongs to the authenticated caller.
 */
@RestController
@RequestMapping("/api/calendar/events")
public class CalendarEventController {

    private final CalendarEventService service;

    public CalendarEventController(CalendarEventService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@Valid @RequestBody CreateEventRequest request, HttpServletRequest http) {
        UserContext.UserInfo user = UserContext.fromRequest(http);
        return service.create(user.getUserId(), user.getEmail(), request);
    }

    @GetMapping
    public List<EventResponse> list(@RequestParam Long workspaceId,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                    HttpServletRequest http) {
        Long userId = UserContext.fromRequest(http).getUserId();
        return service.list(userId, workspaceId, from, to);
    }

    // Declared before "/{id}" so the literal path wins over the {id} template.
    @GetMapping("/current")
    public ResponseEntity<EventResponse> current(@RequestParam Long workspaceId, HttpServletRequest http) {
        Long userId = UserContext.fromRequest(http).getUserId();
        return service.current(userId, workspaceId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public EventResponse get(@PathVariable Long id, HttpServletRequest http) {
        Long userId = UserContext.fromRequest(http).getUserId();
        return service.get(userId, id);
    }

    @PutMapping("/{id}")
    public EventResponse update(@PathVariable Long id,
                                @Valid @RequestBody UpdateEventRequest request,
                                HttpServletRequest http) {
        Long userId = UserContext.fromRequest(http).getUserId();
        return service.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, HttpServletRequest http) {
        Long userId = UserContext.fromRequest(http).getUserId();
        service.delete(userId, id);
    }
}
