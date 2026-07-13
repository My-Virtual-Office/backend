package com.virtualoffice.workspace.dto.request;

import com.virtualoffice.workspace.model.enums.WorkspaceRole;
import jakarta.validation.constraints.Size;

/**
 * ADMIN update of another member's desk membership (role / title / team).
 * All fields are optional (PATCH semantics): a null field is left unchanged.
 * teamId == 0 unassigns the member from any team.
 */
public record UpdateMembershipRequest(
        WorkspaceRole role,
        @Size(max = 120) String title,
        Long teamId) {
}
