package com.res.hms.hospital.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.core.api.PageResponse;
import com.res.hms.hospital.application.dto.HospitalDTO;
import com.res.hms.hospital.application.service.HospitalAggregateService;
import com.res.hms.hospital.application.service.HospitalService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
  private final HospitalAggregateService hospitalAggregateService;

  @PostMapping
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<HospitalDTO> create(@Valid @RequestBody HospitalDTO request) {
    return ApiResponse.success(hospitalAggregateService.saveHospitalAggregate(request));
  }

  @GetMapping
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<PageResponse<HospitalDTO>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String status) {
    return ApiResponse.success(hospitalService.list(page, Math.min(size, 100), name, status));
  }

  @GetMapping("/{hospitalId}")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DOCTOR','RECEPTIONIST','PHARMACIST')")
  public ApiResponse<HospitalDTO> get(@PathVariable UUID hospitalId) {
    return ApiResponse.success(hospitalService.getById(hospitalId));
  }

  @PutMapping("/{hospitalId}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<HospitalDTO> update(
      @PathVariable UUID hospitalId, @Valid @RequestBody HospitalDTO request) {
    request.setId(hospitalId);
    return ApiResponse.success(hospitalAggregateService.saveHospitalAggregate(request));
  }
}
