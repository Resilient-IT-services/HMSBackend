package com.res.hms.clinical.api;

import com.res.hms.clinical.application.prescription.dto.PrescriptionDTO;
import com.res.hms.clinical.application.prescription.dto.PrescriptionSummaryDTO;
import com.res.hms.clinical.application.prescription.service.PrescriptionService;
import com.res.hms.clinical.domain.PrescriptionStatus;
import com.res.hms.core.api.ApiResponse;
import com.res.hms.core.api.PageResponse;
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

@Tag(name = "Prescriptions")
@RestController
@RequestMapping("/api/v1/pharmacy/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

  private final PrescriptionService prescriptionService;

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','NURSE','PHARMACIST','SUPER_ADMIN')")
  public ApiResponse<PageResponse<PrescriptionDTO>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) PrescriptionStatus status) {
    return ApiResponse.success(prescriptionService.list(page, size, search, status));
  }

  @GetMapping("/summary")
  @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','NURSE','PHARMACIST','SUPER_ADMIN')")
  public ApiResponse<PageResponse<PrescriptionSummaryDTO>> listSummary(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) PrescriptionStatus status) {
    return ApiResponse.success(prescriptionService.listSummary(page, size, search, status));
  }

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

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','PHARMACIST','SUPER_ADMIN')")
  public ApiResponse<Void> cancel(@PathVariable UUID id) {
    prescriptionService.cancelPrescription(id);
    return ApiResponse.success(null);
  }
}
