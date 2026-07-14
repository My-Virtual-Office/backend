package com.virtualoffice.tasks.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// The gateway validates the JWT and injects X-User-* after stripping any client-supplied copy,
// so these headers are trusted as-is (mirrors calendar-service / workspace-service UserContext).
public class UserContext {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";
    private static final String HEADER_USER_EMAIL = "X-User-Email";

    private static final String USER = "USER";
    private static final String ADMIN = "ADMIN";

    @Getter
    @AllArgsConstructor
    public static class UserInfo {
        private final Long userId;
        private final String role;
        private final String email;

        public boolean isAdmin() {
            return ADMIN.equalsIgnoreCase(role);
        }

        public boolean isUser() {
            return USER.equalsIgnoreCase(role);
        }
    }

    public static UserInfo fromRequest(HttpServletRequest request) {
        String userIdHeader = request.getHeader(HEADER_USER_ID);
        String role = request.getHeader(HEADER_USER_ROLE);
        String email = request.getHeader(HEADER_USER_EMAIL);

        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing X-User-Id header");
        }

        Long userId;
        try {
            userId = Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Id must be a valid integer");
        }

        if (role == null || (!ADMIN.equalsIgnoreCase(role) && !USER.equalsIgnoreCase(role))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Role must be USER or ADMIN");
        }

        return new UserInfo(userId, role, email);
    }
}
