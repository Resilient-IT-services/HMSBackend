package com.res.hms.inventory.api;

import com.res.hms.core.api.ApiResponse;
import com.res.hms.core.api.PageResponse;
import com.res.hms.inventory.application.dto.InventoryStockDTO;
import com.res.hms.inventory.application.dto.ItemDTO;
import com.res.hms.inventory.application.dto.StockTransactionRequest;
import com.res.hms.inventory.application.service.InventoryService;
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
      @org.springframework.data.web.PageableDefault(sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
          org.springframework.data.domain.Pageable pageable,
      @RequestParam(required = false) String search,
      @RequestParam java.util.Map<String, String> allParams) {
    return ApiResponse.success(inventoryService.listItems(pageable, search, allParams));
  }

  @PostMapping("/stock")
  @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
  public ApiResponse<InventoryStockDTO> recordTransaction(
      @Valid @RequestBody StockTransactionRequest request) {
    return ApiResponse.success(inventoryService.recordTransaction(request));
  }
}
