package com.supplychain.common;

import java.util.List;
import org.springframework.data.domain.Page;

/** The list envelope for every paginated endpoint. Page numbers are zero-based. */
public record PageResponse<T>(List<T> content, int number, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
