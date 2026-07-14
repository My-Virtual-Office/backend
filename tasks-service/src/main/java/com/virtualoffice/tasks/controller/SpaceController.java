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
package com.virtualoffice.tasks.controller;

import com.virtualoffice.tasks.dto.request.CreateSpaceRequest;
import com.virtualoffice.tasks.dto.request.UpdateSpaceRequest;
import com.virtualoffice.tasks.dto.response.SpaceResponse;
import com.virtualoffice.tasks.service.SpaceService;
import com.virtualoffice.tasks.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Team Spaces: create/list/manage the access-controlled spaces that own tasks. */
@RestController
@RequestMapping("/api/tasks/spaces")
public class SpaceController {

    private final SpaceService spaceService;

    public SpaceController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    @GetMapping
    public List<SpaceResponse> list(@RequestParam Long workspaceId, HttpServletRequest http) {
        Long caller = UserContext.fromRequest(http).getUserId();
        return spaceService.listMine(caller, workspaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpaceResponse create(@Valid @RequestBody CreateSpaceRequest request, HttpServletRequest http) {
        Long caller = UserContext.fromRequest(http).getUserId();
        return spaceService.create(caller, request);
    }

    @GetMapping("/{id}")
    public SpaceResponse get(@PathVariable Long id, HttpServletRequest http) {
        Long caller = UserContext.fromRequest(http).getUserId();
        return spaceService.get(caller, id);
    }

    @PatchMapping("/{id}")
    public SpaceResponse update(@PathVariable Long id, @Valid @RequestBody UpdateSpaceRequest request,
                                HttpServletRequest http) {
        Long caller = UserContext.fromRequest(http).getUserId();
        return spaceService.update(caller, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, HttpServletRequest http) {
        Long caller = UserContext.fromRequest(http).getUserId();
        spaceService.delete(caller, id);
    }
}
