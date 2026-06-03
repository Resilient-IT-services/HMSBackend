package com.res.hms.lab.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.lab.application.dto.LabOrderCreateRequest;
import com.res.hms.lab.application.dto.LabOrderResponseDTO;
import com.res.hms.lab.application.dto.LabResultSaveRequest;
import com.res.hms.lab.application.service.LabOrderService;
import com.res.hms.lab.domain.LabOrder;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lab/orders")
@RequiredArgsConstructor
@Tag(name = "Laboratory Orders", description = "Endpoints for managing laboratory orders and queue")
public class LabOrderController {

  private final LabOrderService service;

  @PostMapping
  @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<LabOrder> createOrder(@RequestBody LabOrderCreateRequest request) {
    return ApiResponse.success(service.createOrder(request));
  }

  @GetMapping("/queue")
  @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<List<LabOrderResponseDTO>> getQueue() {
    return ApiResponse.success(service.getQueue());
  }

  @PostMapping("/results")
  @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<Void> saveResults(@RequestBody LabResultSaveRequest request) {
    service.saveResults(request);
    return ApiResponse.success(null);
  }
}
