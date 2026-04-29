package com.res.hms.api;

import com.res.hms.common.api.ApiResponse;
import com.res.hms.vital.dto.VitalSettingDTO;
import com.res.hms.vital.service.VitalSettingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Vital Settings")
@RestController
@RequestMapping("/api/v1/admin/vital-settings")
@RequiredArgsConstructor
public class VitalSettingController {

  private final VitalSettingService vitalSettingService;

  @GetMapping
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<List<VitalSettingDTO>> list() {
    return ApiResponse.success(vitalSettingService.listSettings());
  }

  @PostMapping
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<VitalSettingDTO> save(@Valid @RequestBody VitalSettingDTO setting) {
    return ApiResponse.success(vitalSettingService.saveSetting(setting));
  }
}
