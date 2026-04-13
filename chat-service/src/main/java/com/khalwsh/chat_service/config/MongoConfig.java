package com.khalwsh.chat_service.config;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;

// indexes that need partialFilterExpression can't be done with @CompoundIndex
// because Spring writes null explicitly and mongo sparse only skips *missing* fields
@Configuration
@EnableMongoAuditing
@RequiredArgsConstructor
public class MongoConfig {

    private final MongoTemplate mongoTemplate;

    @PostConstruct
    void ensureIndexes() {
        ensureIdempotencyIndex();
        ensureChannelNameUniqueness();
    }

    // idempotency: (senderId, clientMessageId) must be unique,
    // but only when clientMessageId is actually set
    private void ensureIdempotencyIndex() {
        var messages = mongoTemplate.getCollection("messages");
        messages.createIndex(
                Indexes.compoundIndex(
                        Indexes.ascending("senderId"),
                        Indexes.ascending("clientMessageId")
                ),
                new IndexOptions()
                        .name("idx_sender_clientMsgId")
                        .unique(true)
                        .partialFilterExpression(
                                Filters.and(
                                        Filters.exists("clientMessageId"),
                                        Filters.ne("clientMessageId", null)
                                )
                        )
        );
    }

    // channel names should be unique within a workspace.
    // DMs have workspaceId=null so they're excluded from this constraint.
    private void ensureChannelNameUniqueness() {
        var channels = mongoTemplate.getCollection("channels");
        channels.createIndex(
                Indexes.compoundIndex(
                        Indexes.ascending("workspaceId"),
                        Indexes.ascending("name")
                ),
                new IndexOptions()
                        .name("idx_workspace_name")
                        .unique(true)
                        .partialFilterExpression(
                                Filters.and(
                                        Filters.exists("workspaceId"),
                                        Filters.ne("workspaceId", null)
                                )
                        )
        );
    }
}
