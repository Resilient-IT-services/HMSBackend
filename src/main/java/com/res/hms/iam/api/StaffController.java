package com.res.hms.iam.api;

import java.math.BigDecimal;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.core.api.PageResponse;
import com.res.hms.iam.application.dto.DoctorStaffResponse;
import com.res.hms.iam.application.dto.NurseStaffResponse;
import com.res.hms.iam.application.service.StaffService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffController {

  private final StaffService staffService;

  @GetMapping("/doctors")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<PageResponse<DoctorStaffResponse>> getDoctors(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String department,
      @RequestParam(required = false) String specialization,
      @RequestParam(required = false) BigDecimal minFee,
      @RequestParam(required = false) BigDecimal maxFee,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.success(
        staffService.getDoctors(name, department, specialization, minFee, maxFee, page, size));
  }

  @GetMapping("/nurses")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<PageResponse<NurseStaffResponse>> getNurses(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String department,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.success(staffService.getNurses(name, department, page, size));
  }
}
