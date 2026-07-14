package com.virtualoffice.tasks.service;

import com.virtualoffice.tasks.dto.request.CreateTaskRequest;
import com.virtualoffice.tasks.dto.request.UpdateTaskRequest;
import com.virtualoffice.tasks.dto.response.TaskResponse;

import java.util.List;

/** Task CRUD. The caller's identity (id + email) comes from the gateway-injected X-User-* headers. */
public interface TaskService {

    TaskResponse create(Long callerId, String callerEmail, CreateTaskRequest request);

    List<TaskResponse> list(Long workspaceId, Long assigneeUserId, String status, String q);

    List<TaskResponse> mine(Long callerId, Long workspaceId);

    TaskResponse get(Long id);

    TaskResponse getByNumber(Long workspaceId, Long taskNumber);

    TaskResponse update(Long callerId, String callerEmail, Long id, UpdateTaskRequest request);

    void delete(Long id);
}
