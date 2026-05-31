package com.res.hms.iam.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.core.api.PageResponse;
import com.res.hms.iam.application.dto.DoctorStaffResponse;
import com.res.hms.iam.application.dto.NurseStaffResponse;
import com.res.hms.iam.application.service.StaffService;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffController {

  private final StaffService staffService;

  @GetMapping("/doctors/{doctorId}")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<DoctorStaffResponse> getDoctor(@PathVariable UUID doctorId) {
    return ApiResponse.success(staffService.getDoctor(doctorId));
  }

  @GetMapping("/doctors/selection")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<java.util.List<com.res.hms.iam.application.dto.DoctorSelectionDTO>>
      getDoctorSelection() {
    return ApiResponse.success(staffService.getDoctorSelection());
  }

  @GetMapping("/nurses/{nurseId}")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<NurseStaffResponse> getNurse(@PathVariable UUID nurseId) {
    return ApiResponse.success(staffService.getNurse(nurseId));
  }

  @GetMapping("/doctors")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<PageResponse<DoctorStaffResponse>> getDoctors(
      @org.springframework.data.web.PageableDefault(sort = "user.fullName", direction = org.springframework.data.domain.Sort.Direction.ASC)
          org.springframework.data.domain.Pageable pageable,
      @RequestParam(required = false) String search,
      @RequestParam java.util.Map<String, String> allParams) {
    return ApiResponse.success(staffService.getDoctors(pageable, search, allParams));
  }

  @GetMapping("/nurses")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<PageResponse<NurseStaffResponse>> getNurses(
      @org.springframework.data.web.PageableDefault(sort = "user.fullName", direction = org.springframework.data.domain.Sort.Direction.ASC)
          org.springframework.data.domain.Pageable pageable,
      @RequestParam(required = false) String search,
      @RequestParam java.util.Map<String, String> allParams) {
    return ApiResponse.success(staffService.getNurses(pageable, search, allParams));
  }
}
