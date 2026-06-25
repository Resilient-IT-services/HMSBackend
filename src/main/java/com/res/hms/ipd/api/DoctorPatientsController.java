package com.res.hms.ipd.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.iam.infrastructure.security.SecurityConstants;
import com.res.hms.ipd.application.dto.AdmissionResponseDTO;
import com.res.hms.ipd.application.service.AdmissionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "IPD Doctor Patients")
@RestController
@RequestMapping({"/api/ipd/doctor/my-patients", "/api/v1/ipd/doctor/my-patients"})
@RequiredArgsConstructor
public class DoctorPatientsController {

  private final AdmissionService admissionService;

  @GetMapping
  @PreAuthorize(SecurityConstants.IPD_MY_PATIENTS_VIEW)
  public ApiResponse<List<AdmissionResponseDTO>> getMyPatients() {
    return ApiResponse.success(admissionService.getMyPatients());
  }
}
