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
package com.virtualoffice.room_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Asks for a token for the proximity/zone channel the caller was told to join. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceTokenRequest {

    @NotNull(message = "workspaceId is required")
    private Integer workspaceId;

    /**
     * The channel from VOICE_GROUP_CHANGED; must match what the server assigned this user.
     *
     * Optional: omit it to ask "which channel am I in right now?". VOICE_GROUP_CHANGED only
     * carries *changes*, so a client that connects after its group already formed never sees an
     * event for it — on connect it asks with no channel and gets whatever it is currently in.
     */
    private String channel;
}
