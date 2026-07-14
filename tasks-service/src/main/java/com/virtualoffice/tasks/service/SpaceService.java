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
package com.virtualoffice.tasks.service;

import com.virtualoffice.tasks.dto.request.CreateSpaceRequest;
import com.virtualoffice.tasks.dto.request.UpdateSpaceRequest;
import com.virtualoffice.tasks.dto.response.SpaceResponse;
import com.virtualoffice.tasks.model.TaskSpace;

import java.util.List;

public interface SpaceService {

    /** Spaces in a workspace the caller can access (auto-creates a shared "General" if none exist). */
    List<SpaceResponse> listMine(Long callerId, Long workspaceId);

    SpaceResponse create(Long callerId, CreateSpaceRequest request);

    SpaceResponse get(Long callerId, Long id);

    SpaceResponse update(Long callerId, Long id, UpdateSpaceRequest request);

    void delete(Long callerId, Long id);

    /** Fetch a space and enforce the caller is a member (403 otherwise, 404 if missing). */
    TaskSpace requireAccessibleSpace(Long callerId, Long spaceId);

    /** Get-or-create the workspace's default "General" space, ensuring the caller is a member. */
    TaskSpace ensureDefault(Long callerId, Long workspaceId);
}
