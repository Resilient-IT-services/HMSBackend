package com.res.hms.api;

import com.res.hms.common.api.ApiResponse;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.search.dto.SearchResultDTO;
import com.res.hms.search.service.GlobalSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class GlobalSearchController {

    private final GlobalSearchService globalSearchService;
    private final HospitalContext hospitalContext;

    @GetMapping
    public ApiResponse<List<SearchResultDTO>> search(@RequestParam String query) {
        return ApiResponse.success(globalSearchService.search(
            query,
            hospitalContext.getHospitalIdOrNull(),
            hospitalContext.isSuperAdmin()
        ));
    }
}
