package com.khalwsh.chat_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// generic paginated wrapper — works for channels, threads, messages, etc.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {

    private List<T> content;
    private int totalPages;
    private long totalElements;
    private int currentPage;
}
