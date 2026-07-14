package com.virtualoffice.chat_service.repository;

import com.virtualoffice.chat_service.model.ScheduledMessage;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;

public interface ScheduledMessageRepository extends MongoRepository<ScheduledMessage, ObjectId> {

    @Query("{ 'channelId': ?0, 'senderId': ?1, 'sent': false }")
    List<ScheduledMessage> findPendingForSender(ObjectId channelId, Integer senderId);

    @Query("{ 'sent': false, 'scheduledAt': { $lte: ?0 } }")
    List<ScheduledMessage> findDue(Instant now);
}
