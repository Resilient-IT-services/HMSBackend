package com.res.hms.hospital.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.hospital.domain.Department;
import com.res.hms.hospital.infrastructure.repository.DepartmentRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

  @GetMapping("/all")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<List<Department>> listManagement() {
    return ApiResponse.success(departmentRepository.findAll());
  }

  @PostMapping
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<Department> create(@RequestBody Department department) {
    if (departmentRepository.findByName(department.getName()).isPresent()) {
        throw new com.res.hms.core.exception.ApiException("ALREADY_EXISTS", "Department already exists", HttpStatus.CONFLICT);
    }
    return ApiResponse.success(departmentRepository.save(department));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<Department> update(@PathVariable UUID id, @RequestBody Department department) {
    Department existing = departmentRepository.findById(id)
        .orElseThrow(() -> new com.res.hms.core.exception.ApiException("NOT_FOUND", "Department not found", HttpStatus.NOT_FOUND));
    existing.setName(department.getName());
    existing.setDescription(department.getDescription());
    existing.setActive(department.isActive());
    return ApiResponse.success(departmentRepository.save(existing));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    departmentRepository.deleteById(id);
    return ApiResponse.success(null);
  }
}
