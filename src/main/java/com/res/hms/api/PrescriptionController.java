package com.res.hms.api;

import com.res.hms.common.api.ApiResponse;
import com.res.hms.prescription.dto.PrescriptionDTO;
import com.res.hms.prescription.service.PrescriptionService;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Prescriptions")
@RestController
@RequestMapping("/api/v1/pharmacy/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

  private final PrescriptionService prescriptionService;

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','SUPER_ADMIN')")
  public ApiResponse<PrescriptionDTO> create(@Valid @RequestBody PrescriptionDTO request) {
    return ApiResponse.success(prescriptionService.createPrescription(request));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','NURSE','PHARMACIST','SUPER_ADMIN')")
  public ApiResponse<PrescriptionDTO> get(@PathVariable UUID id) {
    return ApiResponse.success(prescriptionService.getById(id));
  }
}
