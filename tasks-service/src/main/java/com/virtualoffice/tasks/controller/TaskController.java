package com.virtualoffice.tasks.controller;

import com.virtualoffice.tasks.dto.request.CreateTaskRequest;
import com.virtualoffice.tasks.dto.request.UpdateTaskRequest;
import com.virtualoffice.tasks.dto.response.TaskResponse;
import com.virtualoffice.tasks.service.TaskService;
import com.virtualoffice.tasks.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

/**
 * Task CRUD for the workspace board. The caller comes from the gateway-injected X-User-* headers
 * (see {@link UserContext}); {@code createdBy} is always the authenticated caller.
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody CreateTaskRequest request, HttpServletRequest http) {
        UserContext.UserInfo user = UserContext.fromRequest(http);
        return service.create(user.getUserId(), user.getEmail(), request);
    }

    @GetMapping
    public List<TaskResponse> list(@RequestParam Long workspaceId,
                                   @RequestParam(required = false) Long assigneeUserId,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) String q,
                                   HttpServletRequest http) {
        UserContext.fromRequest(http); // require an authenticated caller
        return service.list(workspaceId, assigneeUserId, status, q);
    }

    // Literal paths ("/mine", "/by-number/...") win over the "/{id}" template regardless of order.
    @GetMapping("/mine")
    public List<TaskResponse> mine(@RequestParam Long workspaceId, HttpServletRequest http) {
        Long callerId = UserContext.fromRequest(http).getUserId();
        return service.mine(callerId, workspaceId);
    }

    @GetMapping("/by-number/{workspaceId}/{taskNumber}")
    public TaskResponse byNumber(@PathVariable Long workspaceId,
                                 @PathVariable Long taskNumber,
                                 HttpServletRequest http) {
        UserContext.fromRequest(http);
        return service.getByNumber(workspaceId, taskNumber);
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable Long id, HttpServletRequest http) {
        UserContext.fromRequest(http);
        return service.get(id);
    }

    @PatchMapping("/{id}")
    public TaskResponse update(@PathVariable Long id,
                               @Valid @RequestBody UpdateTaskRequest request,
                               HttpServletRequest http) {
        UserContext.UserInfo user = UserContext.fromRequest(http);
        return service.update(user.getUserId(), user.getEmail(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, HttpServletRequest http) {
        UserContext.fromRequest(http);
        service.delete(id);
    }
}
