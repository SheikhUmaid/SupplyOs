package com.supplychain.common;

import com.supplychain.exception.ApiException;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

/** Query parameters shared by list endpoints. `sort` looks like "name,asc". */
public record PageParams(Integer page, Integer size, String sort) {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    /**
     * @param sortable maps each sort name the API accepts to its entity property, so a client can't
     *     sort on an arbitrary field
     */
    public Pageable toPageable(Map<String, String> sortable, Sort defaultSort) {
        int safePage = page == null ? 0 : Math.max(page, 0);
        int safeSize = size == null ? DEFAULT_SIZE : Math.clamp(size, 1, MAX_SIZE);
        return PageRequest.of(safePage, safeSize, parseSort(sortable, defaultSort));
    }

    private Sort parseSort(Map<String, String> sortable, Sort defaultSort) {
        if (sort == null || sort.isBlank()) {
            return defaultSort;
        }
        String[] parts = sort.split(",");
        String property = sortable.get(parts[0].trim());
        String direction = parts.length > 1 ? parts[1].trim().toLowerCase() : "asc";
        if (property == null || parts.length > 2 || !(direction.equals("asc") || direction.equals("desc"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This list can't be sorted by \"" + sort + "\".");
        }
        return Sort.by(Sort.Direction.fromString(direction), property);
    }
}
