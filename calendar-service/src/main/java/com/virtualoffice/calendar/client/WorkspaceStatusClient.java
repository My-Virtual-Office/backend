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
package com.virtualoffice.calendar.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;

/**
 * Server-to-server client for workspace-service's internal API. Used by the auto-status scheduler
 * to flip a member's Desk status to "In a meeting" while a busy event runs, and to revert it
 * afterwards. Authenticated with the shared {@code X-Internal-Token} (never an end-user JWT).
 */
@Component
@Slf4j
public class WorkspaceStatusClient {

    private final RestClient rest;

    public WorkspaceStatusClient(
            @Value("${workspace.service.base-url:http://localhost:8087}") String baseUrl,
            @Value("${workspace.service.internal-token:dev-internal-token}") String internalToken) {
        this.rest = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Token", internalToken)
                .build();
    }

    /**
     * The user's current custom-status text, for marker-safe revert. An empty Optional means the
     * read FAILED (retry); a present Optional (possibly wrapping null) means the read succeeded.
     */
    public Optional<String> currentStatusText(long workspaceId, long userId) {
        try {
            JoinValidationView v = rest.get()
                    .uri("/api/internal/workspace/{ws}/join-validation/{uid}", workspaceId, userId)
                    .retrieve()
                    .body(JoinValidationView.class);
            return Optional.of(Optional.ofNullable(v).map(JoinValidationView::statusCustomText).orElse(""));
        } catch (Exception e) {
            log.warn("auto-status: could not read status for user {} in ws {}: {}",
                    userId, workspaceId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * PATCH a member's Desk status. Returns true when the change was applied OR when the user has
     * no active desk (404 — nothing to do, so the caller shouldn't retry); false on a transient
     * failure so the scheduler tries again next tick.
     */
    public boolean setStatus(long workspaceId, long userId, String status, String emoji, String text) {
        try {
            rest.patch()
                    .uri("/api/internal/workspace/{ws}/users/{uid}/status", workspaceId, userId)
                    .body(Map.of(
                            "status", status,
                            "statusEmoji", emoji == null ? "" : emoji,
                            "statusCustomText", text == null ? "" : text))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        // swallow 4xx (e.g. 404 no active desk) — treated as "handled" below
                    })
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.warn("auto-status: could not set status for user {} in ws {}: {}",
                    userId, workspaceId, e.getMessage());
            return false;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record JoinValidationView(String statusCustomText) {
    }
}
