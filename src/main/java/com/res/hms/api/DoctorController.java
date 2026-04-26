package com.res.hms.api;

import com.res.hms.common.api.ApiResponse;
import com.res.hms.doctor.dto.DoctorAvailabilityResponse;
import com.res.hms.doctor.service.DoctorService;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
public class DoctorController {

  private final DoctorService doctorService;

  @GetMapping("/{id}/availability")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<DoctorAvailabilityResponse> getAvailability(
      @PathVariable UUID id,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ApiResponse.success(doctorService.getAvailableSlots(id, date));
  }
}
