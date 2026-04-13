package com.khalwsh.chat_service.service;

import java.util.Map;

public interface WebSocketTicketService {

    // generate a one-time ws auth ticket (stores userId + role)
    String createTicket(Integer userId, String role);

    // validate + consume ticket, returns {userId, userRole} or null
    Map<String, Object> validateAndConsumeTicket(String ticket);
}
