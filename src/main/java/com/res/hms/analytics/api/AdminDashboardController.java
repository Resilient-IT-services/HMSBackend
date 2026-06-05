package com.res.hms.analytics.api;

import com.res.hms.analytics.application.dto.AdminDashboardSummaryDTO;
import com.res.hms.analytics.application.service.AdminAnalyticsService;
import com.res.hms.core.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

  private final AdminAnalyticsService adminAnalyticsService;

  @GetMapping("/summary")
  @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('ADMIN') and principal.isSuperAdmin())")
  public ApiResponse<AdminDashboardSummaryDTO> getSummary() {
    return ApiResponse.success(adminAnalyticsService.getDashboardSummary());
  }
}
