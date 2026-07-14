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
package com.virtualoffice.tasks.service.impl;

import com.virtualoffice.tasks.dto.request.CreateSpaceRequest;
import com.virtualoffice.tasks.dto.request.UpdateSpaceRequest;
import com.virtualoffice.tasks.dto.response.SpaceResponse;
import com.virtualoffice.tasks.exception.ResourceNotFoundException;
import com.virtualoffice.tasks.model.TaskSpace;
import com.virtualoffice.tasks.repository.TaskRepository;
import com.virtualoffice.tasks.repository.TaskSpaceRepository;
import com.virtualoffice.tasks.service.SpaceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional
public class SpaceServiceImpl implements SpaceService {

    private static final String DEFAULT_SPACE = "General";

    private final TaskSpaceRepository spaceRepository;
    private final TaskRepository taskRepository;

    public SpaceServiceImpl(TaskSpaceRepository spaceRepository, TaskRepository taskRepository) {
        this.spaceRepository = spaceRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public List<SpaceResponse> listMine(Long callerId, Long workspaceId) {
        List<TaskSpace> spaces = spaceRepository.findAccessible(workspaceId, callerId);
        if (spaces.isEmpty()) {
            // Every workspace member gets access to a shared default space on first visit.
            spaces = List.of(ensureDefault(callerId, workspaceId));
        }
        List<SpaceResponse> out = new ArrayList<>();
        for (TaskSpace s : spaces) {
            out.add(SpaceResponse.from(s, taskRepository.countBySpaceId(s.getId())));
        }
        return out;
    }

    @Override
    public SpaceResponse create(Long callerId, CreateSpaceRequest request) {
        Set<Long> members = new HashSet<>();
        members.add(callerId); // the creator can always access
        if (request.memberUserIds() != null) {
            request.memberUserIds().stream().filter(Objects::nonNull).forEach(members::add);
        }
        TaskSpace space = spaceRepository.save(TaskSpace.builder()
                .workspaceId(request.workspaceId())
                .name(request.name().trim())
                .createdBy(callerId)
                .memberUserIds(members)
                .build());
        return SpaceResponse.from(space, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public SpaceResponse get(Long callerId, Long id) {
        TaskSpace s = requireAccessibleSpace(callerId, id);
        return SpaceResponse.from(s, taskRepository.countBySpaceId(s.getId()));
    }

    @Override
    public SpaceResponse update(Long callerId, Long id, UpdateSpaceRequest request) {
        TaskSpace s = requireAccessibleSpace(callerId, id);
        if (request.name() != null && !request.name().isBlank()) {
            s.setName(request.name().trim());
        }
        if (request.memberUserIds() != null) {
            Set<Long> members = new HashSet<>();
            request.memberUserIds().stream().filter(Objects::nonNull).forEach(members::add);
            members.add(s.getCreatedBy()); // creator can never be removed
            s.setMemberUserIds(members);
        }
        TaskSpace saved = spaceRepository.save(s);
        return SpaceResponse.from(saved, taskRepository.countBySpaceId(saved.getId()));
    }

    @Override
    public void delete(Long callerId, Long id) {
        TaskSpace s = requireAccessibleSpace(callerId, id);
        if (!s.getCreatedBy().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "only the space creator can delete it");
        }
        taskRepository.deleteBySpaceId(id); // remove the space's tasks first
        spaceRepository.delete(s);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskSpace requireAccessibleSpace(Long callerId, Long spaceId) {
        TaskSpace s = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("space not found: " + spaceId));
        if (!s.hasMember(callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "you are not a member of this space");
        }
        return s;
    }

    @Override
    public TaskSpace ensureDefault(Long callerId, Long workspaceId) {
        TaskSpace general = spaceRepository
                .findFirstByWorkspaceIdAndNameOrderByIdAsc(workspaceId, DEFAULT_SPACE)
                .orElseGet(() -> spaceRepository.save(TaskSpace.builder()
                        .workspaceId(workspaceId)
                        .name(DEFAULT_SPACE)
                        .createdBy(callerId)
                        .memberUserIds(new HashSet<>())
                        .build()));
        if (!general.hasMember(callerId)) {
            general.getMemberUserIds().add(callerId);
            general = spaceRepository.save(general);
        }
        return general;
    }
}
