package com.khalwsh.chat_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// enables @Async (used for thread cleanup)
@Configuration
@EnableAsync
public class AsyncConfig {
}
