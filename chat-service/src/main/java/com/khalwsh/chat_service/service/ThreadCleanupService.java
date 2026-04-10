package com.khalwsh.chat_service.service;

import org.bson.types.ObjectId;

public interface ThreadCleanupService {

    // soft-delete all messages in a deleted thread (runs async)
    void cleanupThreadMessages(ObjectId threadId);
}
