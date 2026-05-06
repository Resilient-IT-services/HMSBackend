package com.res.hms.inventory.api;

import com.res.hms.common.api.ApiResponse;
import com.res.hms.common.api.PageResponse;
import com.res.hms.inventory.dto.InventoryStockDTO;
import com.res.hms.inventory.dto.ItemDTO;
import com.res.hms.inventory.dto.StockTransactionRequest;
import com.res.hms.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Inventory")
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

  private final InventoryService inventoryService;

  @PostMapping("/items")
  @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
  public ApiResponse<ItemDTO> createItem(@Valid @RequestBody ItemDTO request) {
    return ApiResponse.success(inventoryService.createItem(request));
  }

  @GetMapping("/items")
  @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
  public ApiResponse<PageResponse<ItemDTO>> listItems(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String search) {
    return ApiResponse.success(inventoryService.listItems(page, size, search));
  }

  @PostMapping("/stock")
  @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
  public ApiResponse<InventoryStockDTO> recordTransaction(
      @Valid @RequestBody StockTransactionRequest request) {
    return ApiResponse.success(inventoryService.recordTransaction(request));
  }
}
