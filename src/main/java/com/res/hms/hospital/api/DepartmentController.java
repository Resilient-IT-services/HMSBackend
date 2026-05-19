package com.res.hms.hospital.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.hospital.application.service.DepartmentService;
import com.res.hms.hospital.domain.Department;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

  private final DepartmentService departmentService;

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<List<String>> listAll() {
    return ApiResponse.success(departmentService.listActiveNames());
  }

  @GetMapping("/all")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<List<Department>> listManagement() {
    return ApiResponse.success(departmentService.listAll());
  }

  @PostMapping
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<Department> create(@RequestBody Department department) {
    return ApiResponse.success(departmentService.create(department));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<Department> update(@PathVariable UUID id, @RequestBody Department department) {
    return ApiResponse.success(departmentService.update(id, department));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    departmentService.delete(id);
    return ApiResponse.success(null);
  }
}
