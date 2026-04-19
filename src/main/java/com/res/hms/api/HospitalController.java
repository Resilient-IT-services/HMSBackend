package com.res.hms.api;

import com.res.hms.common.api.ApiResponse;
import com.res.hms.hospital.dto.CreateHospitalRequest;
import com.res.hms.hospital.dto.HospitalResponse;
import com.res.hms.hospital.service.HospitalService;
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

@Tag(name = "Hospitals")
@RestController
@RequestMapping("/api/v1/hospitals")
@RequiredArgsConstructor
public class HospitalController {

  private final HospitalService hospitalService;

  @PostMapping
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<HospitalResponse> create(@Valid @RequestBody CreateHospitalRequest request) {
    return ApiResponse.success(hospitalService.create(request));
  }

  @GetMapping
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<List<HospitalResponse>> list() {
    return ApiResponse.success(hospitalService.list());
  }
}
