package com.res.hms.hospital.api;

import com.res.hms.clinical.application.vital.dto.VitalRangeConfigDTO;
import com.res.hms.clinical.application.vital.service.VitalRangeConfigService;
import com.res.hms.core.api.ApiResponse;
import com.res.hms.hospital.domain.VitalType;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Vital Range Config")
@RestController
@RequestMapping("/api/v1/admin/vital-ranges")
@RequiredArgsConstructor
public class AdminVitalRangeController {

  private final VitalRangeConfigService service;

  @GetMapping
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<List<VitalRangeConfigDTO>> list() {
    return ApiResponse.success(service.listConfigs());
  }

  @PutMapping("/{vitalType}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<VitalRangeConfigDTO> update(
      @PathVariable VitalType vitalType, @Valid @RequestBody VitalRangeConfigDTO dto) {
    return ApiResponse.success(service.updateConfig(vitalType, dto));
  }
}
