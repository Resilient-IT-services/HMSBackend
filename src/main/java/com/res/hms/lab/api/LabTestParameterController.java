package com.res.hms.lab.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.lab.application.dto.LabReferenceRangeDTO;
import com.res.hms.lab.application.dto.LabTestParameterDTO;
import com.res.hms.lab.application.service.LabReferenceRangeService;
import com.res.hms.lab.application.service.LabTestParameterService;
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

@Tag(name = "Lab Test Parameter Management")
@RestController
@RequestMapping("/api/v1/lab/parameters")
@RequiredArgsConstructor
public class LabTestParameterController {

  private final LabTestParameterService parameterService;
  private final LabReferenceRangeService rangeService;

  @GetMapping("/test/{testId}")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<List<LabTestParameterDTO>> listByTest(@PathVariable UUID testId) {
    return ApiResponse.success(parameterService.listByTest(testId));
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<LabTestParameterDTO> createParameter(@RequestBody LabTestParameterDTO dto) {
    return ApiResponse.success(parameterService.create(dto));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<LabTestParameterDTO> updateParameter(
      @PathVariable UUID id, @RequestBody LabTestParameterDTO dto) {
    return ApiResponse.success(parameterService.update(id, dto));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<Void> deleteParameter(@PathVariable UUID id) {
    parameterService.delete(id);
    return ApiResponse.success(null);
  }

  // Reference Ranges
  @GetMapping("/{parameterId}/ranges")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<List<LabReferenceRangeDTO>> listRanges(@PathVariable UUID parameterId) {
    return ApiResponse.success(rangeService.listByParameter(parameterId));
  }

  @PostMapping("/ranges")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<LabReferenceRangeDTO> createRange(@RequestBody LabReferenceRangeDTO dto) {
    return ApiResponse.success(rangeService.create(dto));
  }

  @PutMapping("/ranges/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<LabReferenceRangeDTO> updateRange(
      @PathVariable UUID id, @RequestBody LabReferenceRangeDTO dto) {
    return ApiResponse.success(rangeService.update(id, dto));
  }

  @DeleteMapping("/ranges/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<Void> deleteRange(@PathVariable UUID id) {
    rangeService.delete(id);
    return ApiResponse.success(null);
  }
}
