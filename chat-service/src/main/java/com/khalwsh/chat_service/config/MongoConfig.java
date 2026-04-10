package com.khalwsh.chat_service.config;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;

// mongo setup + custom indexes
@Configuration
@EnableMongoAuditing
@RequiredArgsConstructor
public class MongoConfig {

    private final MongoTemplate mongoTemplate;

    // unique index on (senderId, clientMessageId) with partialFilter,
    // so rows without clientMessageId are just ignored.
    // can't use @CompoundIndex(sparse) because Spring writes null explicitly
    // and mongo sparse only skips *missing* fields, not null ones.
    @PostConstruct
    void ensureIdempotencyIndex() {
        var collection = mongoTemplate.getCollection("messages");
        collection.createIndex(
                Indexes.compoundIndex(
                        Indexes.ascending("senderId"),
                        Indexes.ascending("clientMessageId")
                ),
                new IndexOptions()
                        .name("idx_sender_clientMsgId")
                        .unique(true)
                        .partialFilterExpression(
                                Filters.exists("clientMessageId")
                        )
        );
    }
}
