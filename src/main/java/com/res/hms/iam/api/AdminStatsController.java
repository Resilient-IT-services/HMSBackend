package com.res.hms.iam.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.iam.application.dto.AdminStatsDTO;
import com.res.hms.iam.application.service.AdminStatsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Metrics")
@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

  private final AdminStatsService adminStatsService;

  @GetMapping
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<AdminStatsDTO> getStats() {
    return ApiResponse.success(adminStatsService.getAdminStats());
  }
}
