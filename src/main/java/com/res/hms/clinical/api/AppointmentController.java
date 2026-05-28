package com.res.hms.clinical.api;

import com.res.hms.clinical.application.appointment.dto.AppointmentDTO;
import com.res.hms.clinical.application.appointment.dto.AppointmentStatsDTO;
import com.res.hms.clinical.application.appointment.dto.AppointmentSummaryDTO;
import com.res.hms.clinical.application.appointment.dto.OPDEncounterRequest;
import com.res.hms.clinical.application.appointment.dto.UpdateAppointmentStatusRequest;
import com.res.hms.clinical.application.appointment.dto.VitalsDTO;
import com.res.hms.clinical.application.appointment.service.AppointmentAggregateService;
import com.res.hms.clinical.application.appointment.service.AppointmentService;
import com.res.hms.clinical.application.appointment.service.OPDEncounterOrchestrator;
import com.res.hms.clinical.domain.AppointmentStatus;
import com.res.hms.core.api.ApiResponse;
import com.res.hms.core.api.PageResponse;
import com.res.hms.core.validation.OnUpdate;
import com.res.hms.iam.application.service.AuthService;
import com.res.hms.iam.infrastructure.security.SecurityConstants;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Appointments")
@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {
  private final AppointmentService appointmentService;
  private final AppointmentAggregateService appointmentAggregateService;
  private final OPDEncounterOrchestrator opdEncounterOrchestrator;
  private final AuthService authService;

  @PostMapping
  @PreAuthorize(SecurityConstants.HAS_ADMIN_OR_RECEPTIONIST)
  public ApiResponse<AppointmentDTO> create(@Valid @RequestBody AppointmentDTO request) {
    return ApiResponse.success(appointmentAggregateService.saveAppointmentAggregate(request));
  }

  @PostMapping("/book-consultation")
  @PreAuthorize(SecurityConstants.HAS_ADMIN_OR_RECEPTIONIST)
  public ApiResponse<AppointmentDTO> bookConsultation(
      @Valid @RequestBody OPDEncounterRequest request) {
    UUID currentUserId = authService.getAuthenticatedPrincipal().userId();
    return ApiResponse.success(
        opdEncounterOrchestrator.bookConsultationWithPayment(request, currentUserId));
  }

  @GetMapping
  @PreAuthorize(SecurityConstants.HAS_RECEPTION_ACCESS)
  public ApiResponse<PageResponse<AppointmentDTO>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to,
      @RequestParam(required = false) UUID patientId,
      @RequestParam(required = false) UUID doctorId,
      @RequestParam(required = false) AppointmentStatus status) {
    return ApiResponse.success(
        appointmentService.list(
            page, Math.min(size, 100), search, from, to, patientId, doctorId, status));
  }

  @GetMapping("/summary")
  @PreAuthorize(SecurityConstants.HAS_RECEPTION_ACCESS)
  public ApiResponse<PageResponse<AppointmentSummaryDTO>> listSummary(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to,
      @RequestParam(required = false) UUID patientId,
      @RequestParam(required = false) UUID doctorId,
      @RequestParam(required = false) AppointmentStatus status) {
    return ApiResponse.success(
        appointmentService.listSummary(
            page, Math.min(size, 100), search, from, to, patientId, doctorId, status));
  }

  @GetMapping("/stats")
  @PreAuthorize(SecurityConstants.HAS_RECEPTION_ACCESS)
  public ApiResponse<AppointmentStatsDTO> getStats(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to,
      @RequestParam(required = false) UUID patientId,
      @RequestParam(required = false) UUID doctorId,
      @RequestParam(required = false) AppointmentStatus status) {
    return ApiResponse.success(
        appointmentService.getStats(search, from, to, patientId, doctorId, status));
  }

  @GetMapping("/{appointmentId}")
  @PreAuthorize(SecurityConstants.HAS_RECEPTION_ACCESS)
  public ApiResponse<AppointmentDTO> get(@PathVariable UUID appointmentId) {
    return ApiResponse.success(appointmentService.getById(appointmentId));
  }

  @PutMapping("/{appointmentId}")
  @PreAuthorize(SecurityConstants.HAS_ADMIN_RECEPTIONIST_OR_DOCTOR)
  public ApiResponse<AppointmentDTO> update(
      @PathVariable UUID appointmentId,
      @Validated(OnUpdate.class) @RequestBody AppointmentDTO request) {
    request.setId(appointmentId);
    return ApiResponse.success(appointmentAggregateService.saveAppointmentAggregate(request));
  }

  @PatchMapping("/{appointmentId}/vitals")
  @PreAuthorize(SecurityConstants.HAS_ADMIN_DOCTOR_OR_NURSE)
  public ApiResponse<AppointmentDTO> updateVitals(
      @PathVariable UUID appointmentId, @Valid @RequestBody VitalsDTO request) {
    return ApiResponse.success(appointmentAggregateService.updateVitals(appointmentId, request));
  }

  @PatchMapping("/{appointmentId}/consultation")
  @PreAuthorize(SecurityConstants.HAS_ADMIN_OR_DOCTOR)
  public ApiResponse<AppointmentDTO> updateConsultation(
      @PathVariable UUID appointmentId, @Valid @RequestBody AppointmentDTO request) {
    return ApiResponse.success(
        appointmentAggregateService.updateConsultation(appointmentId, request));
  }

  @PostMapping("/{appointmentId}/reset-prescription")
  @PreAuthorize(SecurityConstants.HAS_ADMIN)
  public ApiResponse<AppointmentDTO> resetPrescription(@PathVariable UUID appointmentId) {
    return ApiResponse.success(appointmentAggregateService.resetPrescription(appointmentId));
  }

  @PostMapping("/{appointmentId}/status")
  @PreAuthorize(SecurityConstants.HAS_ADMIN_RECEPTIONIST_OR_DOCTOR)
  public ApiResponse<AppointmentDTO> updateStatus(
      @PathVariable UUID appointmentId,
      @Valid @RequestBody UpdateAppointmentStatusRequest request) {
    return ApiResponse.success(appointmentService.updateStatus(appointmentId, request));
  }

  @PutMapping("/{id}/complete")
  @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','SUPER_ADMIN')")
  public ApiResponse<AppointmentDTO> completeConsultation(@PathVariable UUID id) {
    return ApiResponse.success(appointmentAggregateService.completeConsultation(id));
  }

  @PostMapping("/start-next/{doctorId}")
  @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','SUPER_ADMIN')")
  public ApiResponse<AppointmentDTO> startNextConsultation(@PathVariable UUID doctorId) {
    return ApiResponse.success(appointmentAggregateService.startNextConsultation(doctorId));
  }
}
