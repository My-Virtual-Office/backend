/*
 * Copyright (c) 2025 My Virtual Office
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 */
package com.virtualoffice.room_service.controller;

import com.virtualoffice.room_service.dto.request.VoiceTokenRequest;
import com.virtualoffice.room_service.dto.response.JoinRoomResponse;
import com.virtualoffice.room_service.dto.response.ParticipantResponse;
import com.virtualoffice.room_service.dto.response.RoomResponse;
import com.virtualoffice.room_service.dto.response.VoiceTokenResponse;
import com.virtualoffice.room_service.service.PresenceService;
import com.virtualoffice.room_service.service.ProximityService;
import com.virtualoffice.room_service.service.RoomPushService;
import com.virtualoffice.room_service.service.RoomService;
import com.virtualoffice.room_service.util.AgoraTokenBuilder;
import com.virtualoffice.room_service.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomSessionController {

    private final RoomService roomService;
    private final PresenceService presenceService;
    private final RoomPushService roomPushService;
    private final ProximityService proximityService;

    @Value("${room.agora.app-id}")
    private String agoraAppId;

    @Value("${room.agora.app-certificate:}")
    private String agoraAppCertificate;

    @PostMapping("/{id}/join")
    public ResponseEntity<JoinRoomResponse> join(
            @PathVariable String id,
            HttpServletRequest httpRequest) {

        UserContext.UserInfo user = UserContext.fromRequest(httpRequest);
        RoomResponse room = roomService.ensureMemberAndGet(id, user.getUserId());

        ParticipantResponse joined = presenceService.join(id, user.getUserId(), room.getMaxParticipants());
        if (joined != null) {
            roomPushService.participantJoined(id, joined);
        }

        String agoraToken = AgoraTokenBuilder.buildToken(
                agoraAppId, agoraAppCertificate,
                room.getAgoraChannelName(), user.getUserId(), 3600);

        List<ParticipantResponse> participants = presenceService.listParticipants(id);
        return ResponseEntity.ok(JoinRoomResponse.builder()
                .room(room)
                .agoraAppId(agoraAppId)
                .agoraChannelName(room.getAgoraChannelName())
                .agoraToken(agoraToken)
                .participants(participants)
                .build());
    }

    /**
     * Mint an Agora token for the proximity/zone channel this avatar is currently assigned to.
     *
     * VOICE_GROUP_CHANGED only names the channel: it is broadcast to the whole workspace, so a
     * token embedded in it would be readable by everyone, and an Agora token is bound to a uid —
     * handing out someone else's token lets a client join as them. Instead the client asks for its
     * own token here, and we only mint it if room-service actually put this user in that channel.
     *
     * Omitting {@code channel} asks "which channel am I in?" and is how a client recovers the
     * assignment it could not have seen: the broadcast carries only *changes*, so whoever
     * subscribes after their group formed would otherwise sit silent forever. A null
     * {@code agoraChannelName} in the response means "you are in no channel — leave voice".
     */
    @PostMapping("/voice-token")
    public ResponseEntity<VoiceTokenResponse> voiceToken(
            @Valid @RequestBody VoiceTokenRequest request,
            HttpServletRequest httpRequest) {

        UserContext.UserInfo user = UserContext.fromRequest(httpRequest);
        String assigned = proximityService.assignedChannel(request.getWorkspaceId(), user.getUserId());

        // Asking for a specific channel that is not ours is a real error; asking "where am I?"
        // and being nowhere is not.
        if (request.getChannel() != null && !request.getChannel().equals(assigned)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "not assigned to this voice channel");
        }
        if (assigned == null) {
            return ResponseEntity.ok(VoiceTokenResponse.builder().uid(user.getUserId()).build());
        }

        String token = AgoraTokenBuilder.buildToken(
                agoraAppId, agoraAppCertificate, assigned, user.getUserId(), 3600);

        return ResponseEntity.ok(VoiceTokenResponse.builder()
                .agoraAppId(agoraAppId)
                .agoraChannelName(assigned)
                .agoraToken(token)
                .uid(user.getUserId())
                .build());
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leave(
            @PathVariable String id,
            HttpServletRequest httpRequest) {

        UserContext.UserInfo user = UserContext.fromRequest(httpRequest);
        boolean removed = presenceService.leave(id, user.getUserId());
        if (removed) {
            roomPushService.participantLeft(id, user.getUserId());
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/participants")
    public ResponseEntity<List<ParticipantResponse>> participants(
            @PathVariable String id,
            HttpServletRequest httpRequest) {

        UserContext.UserInfo user = UserContext.fromRequest(httpRequest);
        roomService.getRoom(id, user.getUserId());
        return ResponseEntity.ok(presenceService.listParticipants(id));
    }
}
