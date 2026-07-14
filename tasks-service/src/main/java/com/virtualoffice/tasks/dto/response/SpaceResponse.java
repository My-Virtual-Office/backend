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
package com.virtualoffice.tasks.dto.response;

import com.virtualoffice.tasks.model.TaskSpace;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A Team Space with its members and (optional) task count. */
public record SpaceResponse(
        Long id,
        Long workspaceId,
        String name,
        Long createdBy,
        List<Long> memberUserIds,
        long taskCount,
        Instant createdAt) {

    public static SpaceResponse from(TaskSpace s, long taskCount) {
        return new SpaceResponse(
                s.getId(),
                s.getWorkspaceId(),
                s.getName(),
                s.getCreatedBy(),
                new ArrayList<>(s.getMemberUserIds()),
                taskCount,
                s.getCreatedAt());
    }
}
