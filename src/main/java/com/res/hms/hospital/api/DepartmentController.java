package com.res.hms.hospital.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.hospital.domain.Department;
import com.res.hms.hospital.infrastructure.repository.DepartmentRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

  private final DepartmentRepository departmentRepository;

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<List<String>> listAll() {
    return ApiResponse.success(
        departmentRepository.findAll().stream()
            .filter(Department::isActive)
            .map(Department::getName)
            .collect(Collectors.toList()));
  }
}
