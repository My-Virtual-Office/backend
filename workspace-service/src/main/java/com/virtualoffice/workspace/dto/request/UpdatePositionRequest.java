package com.virtualoffice.workspace.dto.request;

import jakarta.validation.constraints.NotNull;

/** Claim/move the caller's own desk to a spot in the virtual office. */
public record UpdatePositionRequest(
        @NotNull Integer positionX,
        @NotNull Integer positionY) {
}
