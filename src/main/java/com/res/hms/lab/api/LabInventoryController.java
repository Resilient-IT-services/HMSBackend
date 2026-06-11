package com.res.hms.lab.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.lab.application.dto.LabInventoryItemDTO;
import com.res.hms.lab.application.service.LabInventoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Lab Inventory Management")
@RestController
@RequestMapping("/api/v1/lab/inventory")
@RequiredArgsConstructor
public class LabInventoryController {

  private final LabInventoryService service;

  @GetMapping
  @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<List<LabInventoryItemDTO>> listInventory() {
    return ApiResponse.success(service.listInventory());
  }

  @PostMapping("/items")
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<LabInventoryItemDTO> createItem(@RequestBody LabInventoryItemDTO dto) {
    return ApiResponse.success(service.createItem(dto));
  }

  @PostMapping("/transactions")
  @PreAuthorize("hasAnyRole('LAB_TECHNICIAN', 'ADMIN', 'SUPER_ADMIN')")
  public ApiResponse<Void> recordTransaction(
      @RequestParam UUID itemId, @RequestParam BigDecimal quantity, @RequestParam String type) {
    service.recordTransaction(itemId, quantity, type);
    return ApiResponse.success(null);
  }
}
