package com.khalwsh.chat_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check endpoint — no authentication required.
 * Used by Docker health checks and monitoring.
 */
@RestController
@RequestMapping("/api/chat")
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
