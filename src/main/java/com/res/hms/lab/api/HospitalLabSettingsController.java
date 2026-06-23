package com.res.hms.lab.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.lab.application.dto.HospitalLabDashboardStatsDTO;
import com.res.hms.lab.application.dto.HospitalLabPackageConfigDTO;
import com.res.hms.lab.application.dto.HospitalLabTestConfigDTO;
import com.res.hms.lab.application.service.HospitalLabSettingsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Hospital Admin Lab Settings")
@RestController
@RequestMapping("/api/v1/hospital/lab")
@RequiredArgsConstructor
public class HospitalLabSettingsController {

  private final HospitalLabSettingsService service;

  @GetMapping("/dashboard/stats")
  @PreAuthorize("hasAnyRole('ADMIN', 'LAB_ADMIN')")
  public ApiResponse<HospitalLabDashboardStatsDTO> getDashboardStats() {
    return ApiResponse.success(service.getDashboardStats());
  }

  @GetMapping("/settings/tests")
  @PreAuthorize("hasAnyRole('ADMIN', 'LAB_ADMIN')")
  public ApiResponse<List<HospitalLabTestConfigDTO>> getAvailableGlobalTests() {
    return ApiResponse.success(service.getAvailableGlobalTests());
  }

  @PutMapping("/settings/tests")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Void> configureTest(@RequestBody HospitalLabTestConfigDTO dto) {
    service.configureTest(dto);
    return ApiResponse.success(null);
  }

  @GetMapping("/settings/packages")
  @PreAuthorize("hasAnyRole('ADMIN', 'LAB_ADMIN')")
  public ApiResponse<List<HospitalLabPackageConfigDTO>> getAvailablePackages() {
    return ApiResponse.success(service.getAvailablePackages());
  }

  @PutMapping("/settings/packages")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Void> configurePackage(@RequestBody HospitalLabPackageConfigDTO dto) {
    service.configurePackage(dto);
    return ApiResponse.success(null);
  }
}
