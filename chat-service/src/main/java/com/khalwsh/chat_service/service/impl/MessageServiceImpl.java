package com.khalwsh.chat_service.service.impl;

import com.khalwsh.chat_service.dto.mapper.DtoMapper;
import com.khalwsh.chat_service.dto.request.SendMessageRequest;
import com.khalwsh.chat_service.dto.response.MessageResponse;
import com.khalwsh.chat_service.dto.response.PaginatedResponse;
import com.khalwsh.chat_service.model.Message;
import com.khalwsh.chat_service.model.MessageType;
import com.khalwsh.chat_service.repository.MessageRepository;
import com.khalwsh.chat_service.service.ChannelService;
import com.khalwsh.chat_service.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ChannelService channelService;

    @Override
    public MessageResponse sendMessage(String channelId, SendMessageRequest request, Integer senderId, String senderRole) {
        // must be a member
        if (!channelService.isMember(channelId, senderId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "you are not a member of this channel");
        }

        // duplicate check: if client sent a clientMessageId, return the existing msg
        if (request.getClientMessageId() != null) {
            Optional<Message> existing = messageRepository.findBySenderIdAndClientMessageId(
                    senderId, request.getClientMessageId());
            if (existing.isPresent()) {
                return DtoMapper.toMessageResponse(existing.get());
            }
        }

        Instant now = Instant.now();

        // blank clientMessageId treated as absent so the partial unique index ignores it
        String clientMsgId = request.getClientMessageId();
        if (clientMsgId != null && clientMsgId.isBlank()) {
            clientMsgId = null;
        }

        Message message = Message.builder()
                .channelId(new ObjectId(channelId))
                .senderId(senderId)
                .senderRole(senderRole != null ? senderRole : "USER")
                .content(request.getContent())
                .type(MessageType.TEXT)
                .threadId(toObjectId(request.getThreadId()))
                .replyToId(toObjectId(request.getReplyToId()))
                .mentions(request.getMentions())
                .clientMessageId(clientMsgId)
                .deleted(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Message saved = messageRepository.save(message);
        return DtoMapper.toMessageResponse(saved);
    }

    // --- channel messages ---

    @Override
    public PaginatedResponse<MessageResponse> getChannelMessages(String channelId, int page, int limit) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt", "_id"));

        Page<Message> messagePage = messageRepository.findChannelMessages(new ObjectId(channelId), pageRequest);

        List<MessageResponse> messages = messagePage.getContent()
                .stream()
                .map(DtoMapper::toMessageResponse)
                .toList();

        return PaginatedResponse.<MessageResponse>builder()
                .content(messages)
                .totalPages(messagePage.getTotalPages())
                .totalElements(messagePage.getTotalElements())
                .currentPage(page)
                .build();
    }

    @Override
    public List<MessageResponse> getChannelMessagesBefore(String channelId, String beforeId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC,  "_id"));

        return messageRepository.findChannelMessagesBefore(new ObjectId(channelId), new ObjectId(beforeId), pageRequest)
                .stream()
                .map(DtoMapper::toMessageResponse)
                .toList();
    }

    @Override
    public List<MessageResponse> getChannelMessagesAfter(String channelId, String afterId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC,  "_id"));

        return messageRepository.findChannelMessagesAfter(new ObjectId(channelId), new ObjectId(afterId), pageRequest)
                .stream()
                .map(DtoMapper::toMessageResponse)
                .toList();
    }

    // --- thread messages ---

    @Override
    public PaginatedResponse<MessageResponse> getThreadMessages(String threadId, int page, int limit) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt", "_id"));

        Page<Message> messagePage = messageRepository.findThreadMessages(new ObjectId(threadId), pageRequest);

        List<MessageResponse> messages = messagePage.getContent()
                .stream()
                .map(DtoMapper::toMessageResponse)
                .toList();

        return PaginatedResponse.<MessageResponse>builder()
                .content(messages)
                .totalPages(messagePage.getTotalPages())
                .totalElements(messagePage.getTotalElements())
                .currentPage(page)
                .build();
    }

    @Override
    public List<MessageResponse> getThreadMessagesBefore(String threadId, String beforeId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt", "_id"));

        return messageRepository.findThreadMessagesBefore(new ObjectId(threadId), new ObjectId(beforeId), pageRequest)
                .stream()
                .map(DtoMapper::toMessageResponse)
                .toList();
    }

    @Override
    public List<MessageResponse> getThreadMessagesAfter(String threadId, String afterId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "createdAt", "_id"));

        return messageRepository.findThreadMessagesAfter(new ObjectId(threadId), new ObjectId(afterId), pageRequest)
                .stream()
                .map(DtoMapper::toMessageResponse)
                .toList();
    }

    // --- edit ---

    @Override
    public MessageResponse editMessage(String messageId, String newContent, Integer requestingUserId, String requestingUserRole) {
        ObjectId msgId = new ObjectId(messageId);
        Message message = messageRepository.findById(msgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "message not found"));

        if (message.getDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot edit a deleted message");
        }

        // only the sender can edit; admins can't edit others'
        if (!message.getSenderId().equals(requestingUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "you can only edit your own messages");
        }

        message.setContent(newContent);
        message.setUpdatedAt(Instant.now());

        Message saved = messageRepository.save(message);
        return DtoMapper.toMessageResponse(saved);
    }

    // --- soft delete ---

    @Override
    public void deleteMessage(String messageId, Integer requestingUserId, String requestingUserRole) {
        ObjectId msgId = new ObjectId(messageId);
        Message message = messageRepository.findById(msgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "message not found"));

        if (message.getDeleted()) {
            return; // already deleted, nothing to do
        }

        boolean isOwner = message.getSenderId().equals(requestingUserId);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(requestingUserRole);

        // owner can delete their own, admins can delete non-admin msgs
        if (!isOwner) {
            if (!isAdmin) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "you can only delete your own messages");
            }
            // admin-on-admin? nope
            boolean senderIsAdmin = "ADMIN".equalsIgnoreCase(message.getSenderRole());
            if (senderIsAdmin) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "admins cannot delete other admins' messages");
            }
        }

        message.setDeleted(true);
        message.setContent(null);
        message.setDeletedAt(Instant.now());
        message.setUpdatedAt(Instant.now());

        messageRepository.save(message);
    }

    // string -> ObjectId, null-safe
    private ObjectId toObjectId(String hexString) {
        return hexString != null ? new ObjectId(hexString) : null;
    }
}
