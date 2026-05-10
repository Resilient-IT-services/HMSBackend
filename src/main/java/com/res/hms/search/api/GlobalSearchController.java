package com.res.hms.search.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.core.api.PageResponse;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.search.application.dto.SearchResultDTO;
import com.res.hms.search.application.service.GlobalSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class GlobalSearchController {

  private final GlobalSearchService globalSearchService;
  private final HospitalContext hospitalContext;

  @GetMapping
  public ApiResponse<PageResponse<SearchResultDTO>> search(
      @RequestParam String query,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.success(
        globalSearchService.search(
            query,
            hospitalContext.getHospitalIdOrNull(),
            hospitalContext.isSuperAdmin(),
            page,
            Math.min(size, 100)));
  }
}
