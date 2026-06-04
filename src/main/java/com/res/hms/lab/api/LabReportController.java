package com.res.hms.lab.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.lab.application.dto.LabReportPreviewDTO;
import com.res.hms.lab.application.service.LabReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lab/reports")
@RequiredArgsConstructor
@Tag(name = "Laboratory Reports", description = "Endpoints for generating and viewing lab reports")
public class LabReportController {

  private final LabReportService service;

  @GetMapping("/preview/{orderId}")
  @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'DOCTOR', 'ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<LabReportPreviewDTO> getReportPreview(@PathVariable UUID orderId) {
    return ApiResponse.success(service.getReportPreview(orderId));
  }
}
