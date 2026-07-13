package com.virtualoffice.chat_service.controller;

import com.mongodb.client.gridfs.model.GridFSFile;
import com.virtualoffice.chat_service.model.Attachment;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Chat file attachments backed by GridFS.
 * Upload is authenticated (POST /api/chat/attachments); download (GET /api/chat/files/{id}) is
 * a public prefix at the gateway so images can load via <img src> (ids are unguessable ObjectIds).
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class AttachmentController {

    private final GridFsTemplate gridFsTemplate;

    @PostMapping("/attachments")
    public Attachment upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "empty file");
        }
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        try {
            ObjectId id = gridFsTemplate.store(file.getInputStream(), file.getOriginalFilename(), contentType);
            return Attachment.builder()
                    .fileId(id.toHexString())
                    .name(file.getOriginalFilename())
                    .contentType(contentType)
                    .size(file.getSize())
                    .build();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "upload failed");
        }
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<InputStreamResource> download(@PathVariable String id) {
        GridFSFile gfsFile = gridFsTemplate.findOne(new Query(where("_id").is(new ObjectId(id))));
        if (gfsFile == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "file not found");
        }
        GridFsResource resource = gridFsTemplate.getResource(gfsFile);
        try {
            String contentType = resource.getContentType();
            return ResponseEntity.ok()
                    .contentType(contentType != null ? MediaType.parseMediaType(contentType)
                            : MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + gfsFile.getFilename() + "\"")
                    .body(new InputStreamResource(resource.getInputStream()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "download failed");
        }
    }
}
