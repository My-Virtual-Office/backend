package com.khalwsh.chat_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

// covers both workspace channels (GROUP) and DMs (DIRECT)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "channels")
@CompoundIndexes({
    @CompoundIndex(name = "idx_dmKey", def = "{'dmKey': 1}", unique = true, sparse = true)
})
public class Channel {

    @Id
    private ObjectId id;

    private String name;

    private ChannelType type;

    private Integer workspaceId;

    @Indexed
    private List<Integer> members;

    private String dmKey;

    private Integer createdBy;

    private Instant createdAt;

    private Instant updatedAt;
}
