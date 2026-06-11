package com.res.hms.lab.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.lab.application.dto.LabMachineDTO;
import com.res.hms.lab.application.service.LabMachineService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Lab Machine Management")
@RestController
@RequestMapping("/api/v1/lab/machines")
@RequiredArgsConstructor
public class LabMachineController {

  private final LabMachineService service;

  @GetMapping
  @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<List<LabMachineDTO>> listAll() {
    return ApiResponse.success(service.listAll());
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<LabMachineDTO> create(@RequestBody LabMachineDTO dto) {
    return ApiResponse.success(service.create(dto));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<LabMachineDTO> update(@PathVariable UUID id, @RequestBody LabMachineDTO dto) {
    return ApiResponse.success(service.update(id, dto));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ApiResponse.success(null);
  }
}
