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
package com.virtualoffice.tasks.repository;

import com.virtualoffice.tasks.model.TaskSpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskSpaceRepository extends JpaRepository<TaskSpace, Long> {

    List<TaskSpace> findByWorkspaceIdOrderByCreatedAtAsc(Long workspaceId);

    // Spaces in a workspace the given user can access.
    @Query("select s from TaskSpace s where s.workspaceId = :workspaceId "
            + "and :userId member of s.memberUserIds order by s.createdAt asc")
    List<TaskSpace> findAccessible(@Param("workspaceId") Long workspaceId, @Param("userId") Long userId);

    Optional<TaskSpace> findFirstByWorkspaceIdAndNameOrderByIdAsc(Long workspaceId, String name);
}
