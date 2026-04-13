package com.khalwsh.chat_service.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;

// reads user identity from nginx-forwarded headers
// nginx already validated the JWT so we just trust these
public class UserContext {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    private static final String USER = "USER";
    private static final String ADMIN = "ADMIN";

    @Getter
    @AllArgsConstructor
    public static class UserInfo {
        private final Integer userId;
        private final String role;

        public boolean isAdmin() {
            return ADMIN.equalsIgnoreCase(role);
        }
        public boolean isUser() { return USER.equalsIgnoreCase(role);}
        public boolean validate() {
            return userId != null && role != null && (isAdmin() || isUser());
        }
    }

    // grab userId and role from headers
    public static UserInfo fromRequest(HttpServletRequest request) {
        String userIdHeader = request.getHeader(HEADER_USER_ID);
        String role = request.getHeader(HEADER_USER_ROLE);

        Integer userId = (userIdHeader != null) ? Integer.parseInt(userIdHeader) : null;

        return new UserInfo(userId, role);
    }
}
