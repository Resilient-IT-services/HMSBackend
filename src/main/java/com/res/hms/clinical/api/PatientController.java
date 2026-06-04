package com.res.hms.clinical.api;

import com.res.hms.clinical.application.patient.dto.PatientDTO;
import com.res.hms.clinical.application.patient.dto.PatientDashboardStatsDTO;
import com.res.hms.clinical.application.patient.dto.PatientHistoryDTO;
import com.res.hms.clinical.application.patient.dto.PatientHistorySummaryDTO;
import com.res.hms.clinical.application.patient.dto.PatientSummaryDTO;
import com.res.hms.clinical.application.patient.service.PatientAggregateService;
import com.res.hms.clinical.application.patient.service.PatientHistoryService;
import com.res.hms.clinical.application.patient.service.PatientService;
import com.res.hms.clinical.application.prescription.dto.PrescriptionDTO;
import com.res.hms.clinical.application.prescription.service.PrescriptionService;
import com.res.hms.core.api.ApiResponse;
import com.res.hms.core.api.PageResponse;
import com.res.hms.iam.infrastructure.security.SecurityConstants;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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

@Tag(name = "Patients")
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

  private final PatientService patientService;
  private final PatientAggregateService patientAggregateService;
  private final PrescriptionService prescriptionService;
  private final PatientHistoryService patientHistoryService;

  @PostMapping
  @PreAuthorize(SecurityConstants.HAS_ADMIN_RECEPTIONIST_OR_NURSE)
  public ApiResponse<PatientDTO> create(@Valid @RequestBody PatientDTO request) {
    return ApiResponse.success(patientAggregateService.savePatientAggregate(request));
  }

  @GetMapping
  @PreAuthorize(SecurityConstants.HAS_ANY_STAFF)
  public ApiResponse<PageResponse<PatientDTO>> list(
      @org.springframework.data.web.PageableDefault(
              sort = "createdAt",
              direction = org.springframework.data.domain.Sort.Direction.DESC)
          org.springframework.data.domain.Pageable pageable,
      @RequestParam(required = false) String search,
      @RequestParam java.util.Map<String, String> allParams) {
    return ApiResponse.success(patientService.list(pageable, search, allParams));
  }

  @GetMapping("/summary")
  @PreAuthorize(SecurityConstants.HAS_ANY_STAFF)
  public ApiResponse<PageResponse<PatientSummaryDTO>> listSummary(
      @org.springframework.data.web.PageableDefault(
              sort = "createdAt",
              direction = org.springframework.data.domain.Sort.Direction.DESC)
          org.springframework.data.domain.Pageable pageable,
      @RequestParam(required = false) String search,
      @RequestParam java.util.Map<String, String> allParams) {
    return ApiResponse.success(patientService.listSummary(pageable, search, allParams));
  }

  @GetMapping("/{patientId}")
  @PreAuthorize(SecurityConstants.HAS_ANY_STAFF)
  public ApiResponse<PatientDTO> get(@PathVariable UUID patientId) {
    return ApiResponse.success(patientService.get(patientId));
  }

  @GetMapping("/{patientId}/summary")
  @PreAuthorize(SecurityConstants.HAS_ANY_STAFF)
  public ApiResponse<PatientSummaryDTO> getSummary(@PathVariable UUID patientId) {
    return ApiResponse.success(patientService.getSummary(patientId));
  }

  @GetMapping("/{patientId}/prescriptions")
  @PreAuthorize(SecurityConstants.HAS_ANY_STAFF)
  public ApiResponse<List<PrescriptionDTO>> getPrescriptions(@PathVariable UUID patientId) {
    return ApiResponse.success(prescriptionService.getPatientPrescriptions(patientId));
  }

  @GetMapping("/{patientId}/full-history")
  @PreAuthorize(SecurityConstants.HAS_ADMIN_DOCTOR_OR_NURSE)
  public ApiResponse<PatientHistoryDTO> getFullHistory(@PathVariable UUID patientId) {
    return ApiResponse.success(patientHistoryService.getFullHistory(patientId));
  }

  @GetMapping("/{patientId}/history-summary")
  @PreAuthorize(SecurityConstants.HAS_ADMIN_DOCTOR_OR_NURSE)
  public ApiResponse<PatientHistorySummaryDTO> getHistorySummary(@PathVariable UUID patientId) {
    return ApiResponse.success(patientHistoryService.getHistorySummary(patientId));
  }

  @PutMapping("/{patientId}")
  @PreAuthorize(SecurityConstants.HAS_ADMIN_RECEPTIONIST_OR_NURSE)
  public ApiResponse<PatientDTO> update(
      @PathVariable UUID patientId, @Valid @RequestBody PatientDTO request) {
    request.setId(patientId);
    return ApiResponse.success(patientAggregateService.savePatientAggregate(request));
  }

  @GetMapping("/dashboard-stats")
  @PreAuthorize(SecurityConstants.HAS_RECEPTION_ACCESS)
  public ApiResponse<PatientDashboardStatsDTO> getDashboardStats() {

    return ApiResponse.success(patientService.getDashboardStats());
  }
}
