package com.biojaveriana.api.common;

import org.springframework.data.domain.Page;

public record Pagination(
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
    public static Pagination from(Page<?> page) {
        return new Pagination(
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }
}
