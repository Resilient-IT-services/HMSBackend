package com.res.hms.api;

import com.res.hms.appointment.dto.AppointmentDTO;
import com.res.hms.appointment.dto.UpdateAppointmentStatusRequest;
import com.res.hms.appointment.service.AppointmentAggregateService;
import com.res.hms.appointment.service.AppointmentService;
import com.res.hms.common.api.ApiResponse;
import com.res.hms.common.api.PageResponse;
import com.res.hms.common.validation.OnCreate;
import com.res.hms.common.validation.OnUpdate;
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

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','DOCTOR','SUPER_ADMIN')")
  public ApiResponse<AppointmentDTO> create(
      @Validated(OnCreate.class) @RequestBody AppointmentDTO request) {
    return ApiResponse.success(appointmentAggregateService.saveAppointmentAggregate(request));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','NURSE','DOCTOR','SUPER_ADMIN')")
  public ApiResponse<PageResponse<AppointmentDTO>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
    return ApiResponse.success(appointmentService.list(page, Math.min(size, 100), search, date));
  }

  @GetMapping("/stats")
  @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','NURSE','DOCTOR','SUPER_ADMIN')")
  public ApiResponse<com.res.hms.appointment.dto.AppointmentStatsDTO> getStats(
      @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
    return ApiResponse.success(appointmentService.getStats(date != null ? date : java.time.LocalDate.now()));
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
      @Valid @RequestBody com.res.hms.appointment.dto.VitalsDTO request) {
    return ApiResponse.success(appointmentAggregateService.updateVitals(appointmentId, request));
  }

  @PatchMapping("/{appointmentId}/consultation")
  @PreAuthorize("hasAnyRole('ADMIN','DOCTOR','SUPER_ADMIN')")
  public ApiResponse<AppointmentDTO> updateConsultation(
      @PathVariable UUID appointmentId, @Valid @RequestBody AppointmentDTO request) {
    return ApiResponse.success(
        appointmentAggregateService.updateConsultation(appointmentId, request));
  }

  @PostMapping("/{appointmentId}/status")
  @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','DOCTOR','SUPER_ADMIN')")
  public ApiResponse<AppointmentDTO> updateStatus(
      @PathVariable UUID appointmentId,
      @Valid @RequestBody UpdateAppointmentStatusRequest request) {
    return ApiResponse.success(appointmentService.updateStatus(appointmentId, request));
  }
}
