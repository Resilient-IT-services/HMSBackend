package com.res.hms.api;

import com.res.hms.common.api.ApiResponse;
import com.res.hms.common.api.PageResponse;
import com.res.hms.hospital.dto.HospitalDTO;
import com.res.hms.hospital.service.HospitalAggregateService;
import com.res.hms.hospital.service.HospitalService;
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

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<HospitalDTO> get(@PathVariable UUID id) {
    return ApiResponse.success(hospitalService.getById(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<HospitalDTO> update(
      @PathVariable UUID id, @Valid @RequestBody HospitalDTO request) {
    request.setId(id);
    return ApiResponse.success(hospitalAggregateService.saveHospitalAggregate(request));
  }
}
