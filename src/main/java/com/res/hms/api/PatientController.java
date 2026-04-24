package com.res.hms.api;

import com.res.hms.common.api.ApiResponse;
import com.res.hms.common.api.PageResponse;
import com.res.hms.patient.dto.CreatePatientRequest;
import com.res.hms.patient.dto.PatientResponse;
import com.res.hms.patient.service.PatientService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Patients")
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

  private final PatientService patientService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','NURSE')")
  public ApiResponse<PatientResponse> create(@Valid @RequestBody CreatePatientRequest request) {
    return ApiResponse.success(patientService.create(request));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','NURSE','DOCTOR')")
  public ApiResponse<PageResponse<PatientResponse>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String search) {
    return ApiResponse.success(patientService.list(page, Math.min(size, 100), search));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','NURSE','DOCTOR')")
  public ApiResponse<PatientResponse> get(@PathVariable UUID id) {
    return ApiResponse.success(patientService.get(id));
  }
}
