package com.khalwsh.chat_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// health check — no auth required, used by Docker / monitoring
@RestController
@RequestMapping("/api/chat")
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
