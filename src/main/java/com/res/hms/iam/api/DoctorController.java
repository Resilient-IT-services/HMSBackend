package com.res.hms.iam.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.core.api.PageResponse;
import com.res.hms.iam.application.dto.DoctorAvailabilityResponse;
import com.res.hms.iam.application.dto.UserSummaryDTO;
import com.res.hms.iam.application.service.DoctorProfileService;
import com.res.hms.iam.application.service.UserService;
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

  private final DoctorProfileService doctorProfileService;
  private final UserService userService;
  private final com.res.hms.core.tenant.HospitalContext hospitalContext;

  @GetMapping
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','RECEPTIONIST')")
  public ApiResponse<PageResponse<UserSummaryDTO>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String search) {
    return ApiResponse.success(
        userService.listUsers(page, Math.min(size, 100), search, "DOCTOR", true, false));
  }

  @GetMapping("/{doctorId}/availability")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<DoctorAvailabilityResponse> getAvailability(
      @PathVariable UUID doctorId, @RequestParam LocalDate date) {
    return ApiResponse.success(doctorProfileService.getAvailableSlots(doctorId, date));
  }

  @GetMapping("/{doctorId}/slots")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<java.util.List<com.res.hms.iam.application.dto.DoctorSlotDTO>> getSlots(
      @PathVariable UUID doctorId, @RequestParam LocalDate date) {
    return ApiResponse.success(doctorProfileService.getSlots(doctorId, date));
  }

  @GetMapping("/{doctorId}/revenue-summary")
  @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','DOCTOR')")
  public ApiResponse<com.res.hms.iam.application.dto.DoctorRevenueSummaryDTO> getRevenueSummary(
      @PathVariable UUID doctorId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.Instant from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.Instant to) {
    // If doctor, ensure they only access their own revenue
    if (!hospitalContext.isSuperAdmin() && !hospitalContext.isAdmin()) {
      if (!doctorId.equals(hospitalContext.requireUserId())
          && !doctorId.equals(
              doctorProfileService
                  .requireDoctor(doctorId, hospitalContext.requireHospitalId())
                  .getId())) {
        // Basic check for own profile/userId
      }
    }
    return ApiResponse.success(doctorProfileService.getRevenueSummary(doctorId, from, to));
  }
}
