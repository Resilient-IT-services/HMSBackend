package com.res.hms.api;

import com.res.hms.common.api.ApiResponse;
import com.res.hms.common.api.PageResponse;
import com.res.hms.doctor.dto.DoctorAvailabilityResponse;
import com.res.hms.doctor.service.DoctorProfileService;
import com.res.hms.user.dto.UserSummaryDTO;
import com.res.hms.user.service.UserService;
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

  private final DoctorProfileService DoctorProfileService;
  private final UserService userService;

  @GetMapping
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','RECEPTIONIST')")
  public ApiResponse<PageResponse<UserSummaryDTO>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String search) {
    return ApiResponse.success(
        userService.listUsers(page, Math.min(size, 100), search, "DoctorProfile", null, false));
  }

  @GetMapping("/{doctorId}/availability")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<DoctorAvailabilityResponse> getAvailability(
      @PathVariable UUID doctorId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ApiResponse.success(DoctorProfileService.getAvailableSlots(doctorId, date));
  }
}
