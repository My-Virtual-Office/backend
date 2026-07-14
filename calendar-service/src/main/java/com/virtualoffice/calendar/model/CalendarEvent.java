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
package com.virtualoffice.calendar.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * A calendar event owned by a single user within a workspace. When {@code busy} is true and the
 * event window contains "now", the auto-status job flips the owner's Desk to "In a meeting".
 */
@Entity
@Table(name = "calendar_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long workspaceId;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private Instant startTime;

    @Column(nullable = false)
    private Instant endTime;

    // A "busy" event marks the owner unavailable (drives auto-status); a "free" event does not.
    @Builder.Default
    @Column(nullable = false)
    private boolean busy = true;

    // The owner's email, captured from the trusted X-User-Email header, so reminders can be sent.
    private String creatorEmail;

    // Minutes before startTime to send a reminder (null = no reminder).
    private Integer reminderMinutes;

    // Set once the reminder has been dispatched, so it fires at most once.
    private Instant reminderSentAt;

    // Invited attendees: userId -> email. Everyone here is reminded/notified alongside the owner.
    @jakarta.persistence.ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @jakarta.persistence.CollectionTable(
            name = "calendar_event_attendee",
            joinColumns = @jakarta.persistence.JoinColumn(name = "event_id"))
    @jakarta.persistence.MapKeyColumn(name = "attendee_user_id")
    @jakarta.persistence.Column(name = "attendee_email")
    @Builder.Default
    private java.util.Map<Long, String> attendees = new java.util.HashMap<>();

    // Auto-status bookkeeping (calendar auto-status scheduler). One-shot flags so the owner's
    // Desk is flipped to "In a meeting" once when a busy event starts and reverted once when it ends.
    @Column(nullable = false)
    @Builder.Default
    private boolean statusApplied = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean statusCleared = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
