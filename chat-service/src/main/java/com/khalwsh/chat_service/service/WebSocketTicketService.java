package com.khalwsh.chat_service.service;

public interface WebSocketTicketService {

    // generate a one-time ws auth ticket
    String createTicket(Integer userId);

    // validate + consume ticket, returns userId or null
    Integer validateAndConsumeTicket(String ticket);
}
