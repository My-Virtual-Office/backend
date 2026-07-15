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
package com.virtualoffice.room_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Everything a client needs to join a proximity/zone Agora channel. The App ID rides along so the
 * clients need no build-time Agora config — the server is the single source of truth for it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceTokenResponse {
    private String agoraAppId;
    private String agoraChannelName;
    /** Empty when no App Certificate is configured (App-ID-only mode). */
    private String agoraToken;
    /** The uid the token is bound to; the client must join with exactly this. */
    private Integer uid;
}
