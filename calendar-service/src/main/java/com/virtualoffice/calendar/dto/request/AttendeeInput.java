package com.virtualoffice.calendar.dto.request;

import jakarta.validation.constraints.NotNull;

/** An invited attendee. The frontend supplies the email (it already has it from the member list). */
public record AttendeeInput(
        @NotNull Long userId,
        String email) {
}
