package com.res.hms.api;

import com.res.hms.common.api.ApiResponse;
import com.res.hms.common.api.PageResponse;
import com.res.hms.domain.HospitalStatus;
import com.res.hms.hospital.dto.CreateHospitalRequest;
import com.res.hms.hospital.dto.HospitalResponse;
import com.res.hms.hospital.service.HospitalService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
  public ApiResponse<PageResponse<HospitalResponse>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) HospitalStatus status) {
    return ApiResponse.success(hospitalService.list(page, Math.min(size, 100), name, status));
  }

  @org.springframework.web.bind.annotation.PutMapping("/{id}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<HospitalResponse> update(
      @org.springframework.web.bind.annotation.PathVariable UUID id,
      @Valid @RequestBody CreateHospitalRequest request) {
    return ApiResponse.success(hospitalService.update(id, request));
  }
}
