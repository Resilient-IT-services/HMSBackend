package com.res.hms.clinical.api;

import com.res.hms.clinical.application.appointment.dto.AppointmentDTO;
import com.res.hms.clinical.application.appointment.dto.AppointmentSummaryDTO;
import com.res.hms.clinical.application.appointment.dto.UpdateAppointmentStatusRequest;
import com.res.hms.clinical.application.appointment.service.AppointmentAggregateService;
import com.res.hms.clinical.application.appointment.service.AppointmentService;
import com.res.hms.core.api.ApiResponse;
import com.res.hms.core.api.PageResponse;
import com.res.hms.core.validation.OnUpdate;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
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
    private final com.res.hms.clinical.application.appointment.service.OPDEncounterOrchestrator
        opdEncounterOrchestrator;
    private final com.res.hms.iam.application.service.AuthService authService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','SUPER_ADMIN')")
    public ApiResponse<AppointmentDTO> create(@Valid @RequestBody AppointmentDTO request) {
        return ApiResponse.success(appointmentAggregateService.saveAppointmentAggregate(request));
    }

    @PostMapping("/book-consultation")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','SUPER_ADMIN')")
    public ApiResponse<AppointmentDTO> bookConsultation(
        @Valid @RequestBody
        com.res.hms.clinical.application.appointment.dto.OPDEncounterRequest request) {
        java.util.UUID currentUserId = authService.getAuthenticatedPrincipal().userId();
        return ApiResponse.success(
            opdEncounterOrchestrator.bookConsultationWithPayment(request, currentUserId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','NURSE','DOCTOR','SUPER_ADMIN')")
    public ApiResponse<PageResponse<AppointmentDTO>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String search,
        @RequestParam(required = false)
        @org.springframework.format.annotation.DateTimeFormat(
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
        java.time.Instant from,
        @RequestParam(required = false)
        @org.springframework.format.annotation.DateTimeFormat(
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
        java.time.Instant to,
        @RequestParam(required = false) UUID patientId,
        @RequestParam(required = false) UUID doctorId,
        @RequestParam(required = false) com.res.hms.clinical.domain.AppointmentStatus status) {
        return ApiResponse.success(
            appointmentService.list(page, Math.min(size, 100), search, from, to, patientId, doctorId, status));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','NURSE','DOCTOR','SUPER_ADMIN')")
    public ApiResponse<PageResponse<AppointmentSummaryDTO>> listSummary(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String search,
        @RequestParam(required = false)
        @org.springframework.format.annotation.DateTimeFormat(
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
        java.time.Instant from,
        @RequestParam(required = false)
        @org.springframework.format.annotation.DateTimeFormat(
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
        java.time.Instant to,
        @RequestParam(required = false) UUID patientId,
        @RequestParam(required = false) UUID doctorId,
        @RequestParam(required = false) com.res.hms.clinical.domain.AppointmentStatus status) {
        return ApiResponse.success(
            appointmentService.listSummary(page, Math.min(size, 100), search, from, to, patientId, doctorId, status));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','NURSE','DOCTOR','SUPER_ADMIN')")
    public ApiResponse<com.res.hms.clinical.application.appointment.dto.AppointmentStatsDTO> getStats(
        @RequestParam(required = false) String search,
        @RequestParam(required = false)
        @org.springframework.format.annotation.DateTimeFormat(
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
        java.time.Instant from,
        @RequestParam(required = false)
        @org.springframework.format.annotation.DateTimeFormat(
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
        java.time.Instant to,
        @RequestParam(required = false) UUID patientId,
        @RequestParam(required = false) UUID doctorId,
        @RequestParam(required = false) com.res.hms.clinical.domain.AppointmentStatus status) {
        return ApiResponse.success(appointmentService.getStats(search, from, to, patientId, doctorId, status));
    }

    @GetMapping("/{appointmentId}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','NURSE','DOCTOR','SUPER_ADMIN')")
    public ApiResponse<AppointmentDTO> get(@PathVariable UUID appointmentId) {
        return ApiResponse.success(appointmentService.getById(appointmentId));
    }

    @PutMapping("/{appointmentId}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','DOCTOR','SUPER_ADMIN')")
    public ApiResponse<AppointmentDTO> update(
        @PathVariable UUID appointmentId,
        @Validated(OnUpdate.class) @RequestBody AppointmentDTO request) {
        request.setId(appointmentId);
        return ApiResponse.success(appointmentAggregateService.saveAppointmentAggregate(request));
    }

    @PatchMapping("/{appointmentId}/vitals")
    @PreAuthorize("hasAnyRole('ADMIN','NURSE','DOCTOR','SUPER_ADMIN')")
    public ApiResponse<AppointmentDTO> updateVitals(
        @PathVariable UUID appointmentId,
        @Valid @RequestBody com.res.hms.clinical.application.appointment.dto.VitalsDTO request) {
        return ApiResponse.success(appointmentAggregateService.updateVitals(appointmentId, request));
    }

    @PatchMapping("/{appointmentId}/consultation")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','SUPER_ADMIN')")
    public ApiResponse<AppointmentDTO> updateConsultation(
        @PathVariable UUID appointmentId, @Valid @RequestBody AppointmentDTO request) {
        return ApiResponse.success(
            appointmentAggregateService.updateConsultation(appointmentId, request));
    }

    @PostMapping("/{appointmentId}/reset-prescription")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<AppointmentDTO> resetPrescription(@PathVariable UUID appointmentId) {
        return ApiResponse.success(appointmentAggregateService.resetPrescription(appointmentId));
    }

    @PostMapping("/{appointmentId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','DOCTOR','SUPER_ADMIN')")
    public ApiResponse<AppointmentDTO> updateStatus(
        @PathVariable UUID appointmentId,
        @Valid @RequestBody UpdateAppointmentStatusRequest request) {
        return ApiResponse.success(appointmentService.updateStatus(appointmentId, request));
    }
}
