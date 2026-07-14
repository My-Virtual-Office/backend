package com.virtualoffice.chat_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A file attached to a message. The bytes live in GridFS; this holds the reference + metadata.
 * Download at GET /api/chat/files/{fileId}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {
    private String fileId;
    private String name;
    private String contentType;
    private Long size;
}
