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

import com.res.hms.core.api.PageResponse;
import com.res.hms.lab.application.dto.LabQueueItemDTO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.UUID;

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

  @GetMapping("/queue/paged")
  @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'PATHOLOGIST', 'ADMIN')")
  public ApiResponse<PageResponse<LabQueueItemDTO>> getPagedQueue(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.success(service.getLabQueue(status, page, size));
  }

  @PostMapping("/tests/{orderTestId}/collect")
  @PreAuthorize("hasRole('LAB_TECHNICIAN')")
  public ApiResponse<String> collectSample(@PathVariable UUID orderTestId) {
    return ApiResponse.success(service.collectSample(orderTestId));
  }

  @GetMapping("/tests/{orderTestId}/parameters")
  @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'PATHOLOGIST', 'ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<List<com.res.hms.lab.application.dto.LabTestParameterDTO>> getParametersByOrderTest(@PathVariable UUID orderTestId) {
    return ApiResponse.success(service.getParametersForOrderTest(orderTestId));
  }

  @PostMapping("/results")
  @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<Void> saveResults(@RequestBody LabResultSaveRequest request) {
    service.saveResults(request);
    return ApiResponse.success(null);
  }
}
