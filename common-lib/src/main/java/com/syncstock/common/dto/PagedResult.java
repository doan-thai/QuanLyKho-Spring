package com.syncstock.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResult<T> {

    @Builder.Default
    private long totalRecords = 0L;

    @Builder.Default
    private int pageNumber = 1;

    @Builder.Default
    private int pageSize = 10;

    @Builder.Default
    private int totalPages = 0;

    @Builder.Default
    private List<T> data = Collections.emptyList();
}

