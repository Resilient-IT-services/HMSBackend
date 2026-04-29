package com.res.hms.api;

import com.res.hms.common.api.ApiResponse;
import com.res.hms.domain.VitalSetting;
import com.res.hms.repository.VitalSettingRepository;
import com.res.hms.common.tenant.HospitalContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Vital Settings")
@RestController
@RequestMapping("/api/v1/admin/vital-settings")
@RequiredArgsConstructor
public class VitalSettingController {

  private final VitalSettingRepository vitalSettingRepository;
  private final HospitalContext hospitalContext;

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
  public ApiResponse<List<VitalSetting>> list() {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return ApiResponse.success(vitalSettingRepository.findAllByHospitalId(hospitalId));
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
  public ApiResponse<VitalSetting> save(@Valid @RequestBody VitalSetting setting) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    setting.setHospitalId(hospitalId);
    
    vitalSettingRepository.findByHospitalIdAndVitalName(hospitalId, setting.getVitalName())
        .ifPresent(existing -> setting.setId(existing.getId()));
        
    return ApiResponse.success(vitalSettingRepository.save(setting));
  }
}
