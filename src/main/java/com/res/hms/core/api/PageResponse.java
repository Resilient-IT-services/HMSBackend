package com.res.hms.core.api;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {

  public static <T> PageResponse<T> from(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }

  public static <T> PageResponse<T> single(T data) {
    return new PageResponse<>(
        List.of(data), // content
        0, // page
        1, // size
        1, // totalElements
        1 // totalPages
        );
  }
}
