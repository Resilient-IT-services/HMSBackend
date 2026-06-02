package com.res.hms.laboratory.api;

import com.res.hms.audit.domain.DomainEntity;
import com.res.hms.core.api.ApiResponse;
import com.res.hms.core.audit.Auditable;
import com.res.hms.laboratory.application.dto.LaboratoryTestDTO;
import com.res.hms.laboratory.application.service.LaboratoryTestService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/laboratory/tests")
@RequiredArgsConstructor
public class LaboratoryTestController {

  private final LaboratoryTestService laboratoryTestService;

  @GetMapping
  @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DOCTOR', 'NURSE', 'LAB_TECHNICIAN')")
  public ApiResponse<List<LaboratoryTestDTO>> list() {
    return ApiResponse.success(laboratoryTestService.listAll());
  }

  @PostMapping
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  @Auditable(entity = DomainEntity.LABORATORY_TEST, action = "CREATE")
  public ApiResponse<LaboratoryTestDTO> create(@Valid @RequestBody LaboratoryTestDTO request) {
    return ApiResponse.success(laboratoryTestService.create(request));
  }
}
