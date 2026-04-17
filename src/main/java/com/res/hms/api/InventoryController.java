package com.res.hms.api;

import com.res.hms.common.api.ApiResponse;
import com.res.hms.common.api.PageResponse;
import com.res.hms.inventory.dto.CreateItemRequest;
import com.res.hms.inventory.dto.InventoryStockResponse;
import com.res.hms.inventory.dto.ItemResponse;
import com.res.hms.inventory.dto.UpdateInventoryStockRequest;
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

import java.util.List;

@Tag(name = "Inventory")
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public ApiResponse<ItemResponse> createItem(@Valid @RequestBody CreateItemRequest request) {
        return ApiResponse.success(inventoryService.createItem(request));
    }

    @GetMapping("/items")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public ApiResponse<PageResponse<ItemResponse>> listItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(inventoryService.listItems(page, Math.min(size, 100)));
    }

    @PostMapping("/stock")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public ApiResponse<InventoryStockResponse> updateStock(@Valid @RequestBody UpdateInventoryStockRequest request) {
        return ApiResponse.success(inventoryService.updateStock(request));
    }

    @GetMapping("/stock")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public ApiResponse<List<InventoryStockResponse>> listStock() {
        return ApiResponse.success(inventoryService.listStock());
    }
}
