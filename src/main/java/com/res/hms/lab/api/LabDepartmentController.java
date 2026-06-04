package com.res.hms.lab.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.lab.application.dto.LabDepartmentDTO;
import com.res.hms.lab.application.service.LabDepartmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Lab Department Management")
@RestController
@RequestMapping("/api/v1/lab/departments")
@RequiredArgsConstructor
public class LabDepartmentController {

  private final LabDepartmentService service;

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<List<LabDepartmentDTO>> listAll() {
    return ApiResponse.success(service.listAll());
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<LabDepartmentDTO> create(@RequestBody LabDepartmentDTO dto) {
    return ApiResponse.success(service.create(dto));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<LabDepartmentDTO> update(
      @PathVariable UUID id, @RequestBody LabDepartmentDTO dto) {
    return ApiResponse.success(service.update(id, dto));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ApiResponse.success(null);
  }
}
