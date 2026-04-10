package com.khalwsh.chat_service.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;

// reads user identity from nginx-forwarded headers
// nginx already validated the JWT so we just trust these
public class UserContext {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    @Getter
    @AllArgsConstructor
    public static class UserInfo {
        private final Integer userId;
        private final String role;

        public boolean isAdmin() {
            return "ADMIN".equalsIgnoreCase(role);
        }
    }

    // grab userId and role from headers
    public static UserInfo fromRequest(HttpServletRequest request) {
        String userIdHeader = request.getHeader(HEADER_USER_ID);
        String roleHeader = request.getHeader(HEADER_USER_ROLE);

        Integer userId = (userIdHeader != null) ? Integer.parseInt(userIdHeader) : null;
        String role = (roleHeader != null) ? roleHeader : "USER";

        return new UserInfo(userId, role);
    }
}
