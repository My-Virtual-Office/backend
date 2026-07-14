package com.virtualoffice.chat_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** A message queued to be delivered to a channel at a future time. */
@Document(collection = "scheduled_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledMessage {

    @Id
    private ObjectId id;

    private ObjectId channelId;
    private Integer senderId;
    private String senderRole;
    private String content;
    private Instant scheduledAt;

    @Builder.Default
    private boolean sent = false;

    private Instant createdAt;
}
