package com.res.hms.api;

import com.res.hms.common.api.ApiResponse;
import com.res.hms.common.api.PageResponse;
import com.res.hms.doctor.dto.CreateDoctorRequest;
import com.res.hms.doctor.dto.DoctorResponse;
import com.res.hms.doctor.service.DoctorService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Doctors")
@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
public class DoctorController {

  private final DoctorService doctorService;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<DoctorResponse> create(@Valid @RequestBody CreateDoctorRequest request) {
    return ApiResponse.success(doctorService.create(request));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','RECEPTIONIST','NURSE','DOCTOR')")
  public ApiResponse<PageResponse<DoctorResponse>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String search) {
    return ApiResponse.success(doctorService.list(page, Math.min(size, 100), search));
  }
}
