package com.res.hms.iam.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.iam.application.service.SpecializationService;
import com.res.hms.iam.domain.Specialization;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/specializations")
@RequiredArgsConstructor
public class SpecializationController {

  private final SpecializationService specializationService;

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<List<String>> listAll() {
    return ApiResponse.success(specializationService.listActiveNames());
  }

  @GetMapping("/all")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<List<Specialization>> listManagement() {
    return ApiResponse.success(specializationService.listAll());
  }

  @PostMapping
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<Specialization> create(@RequestBody Specialization specialization) {
    return ApiResponse.success(specializationService.create(specialization));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<Specialization> update(
      @PathVariable UUID id, @RequestBody Specialization specialization) {
    return ApiResponse.success(specializationService.update(id, specialization));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    specializationService.delete(id);
    return ApiResponse.success(null);
  }
}
