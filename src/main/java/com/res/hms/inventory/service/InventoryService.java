package com.res.hms.inventory.service;

import com.res.hms.common.api.PageResponse;
import com.res.hms.common.audit.Auditable;
import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.domain.InventoryStock;
import com.res.hms.domain.InventoryTransaction;
import com.res.hms.domain.InventoryTransactionType;
import com.res.hms.domain.Item;
import com.res.hms.inventory.dto.CreateItemRequest;
import com.res.hms.inventory.dto.InventoryStockResponse;
import com.res.hms.inventory.dto.ItemResponse;
import com.res.hms.inventory.dto.UpdateInventoryStockRequest;
import com.res.hms.inventory.repository.InventoryStockRepository;
import com.res.hms.inventory.repository.InventoryTransactionRepository;
import com.res.hms.inventory.repository.ItemRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

  private final ItemRepository itemRepository;
  private final InventoryStockRepository inventoryStockRepository;
  private final InventoryTransactionRepository inventoryTransactionRepository;
  private final HospitalContext hospitalContext;

  @Transactional
  @Auditable(entity = "item", action = "CREATE")
  public ItemResponse createItem(CreateItemRequest request) {
    Long hospitalId = hospitalContext.requireHospitalId();
    Item item = new Item();
    item.setHospitalId(hospitalId);
    item.setName(request.name());
    item.setCategory(request.category());
    item.setUnit(request.unit());
    itemRepository.save(item);

    return toItemResponse(item);
  }

  public PageResponse<ItemResponse> listItems(int page, int size) {
    Long hospitalId = hospitalContext.requireHospitalId();
    return PageResponse.from(
        itemRepository
            .findAllByHospitalIdOrderByCreatedAtDesc(hospitalId, PageRequest.of(page, size))
            .map(this::toItemResponse));
  }

  @Transactional
  @Auditable(entity = "inventory_stock", action = "UPDATE")
  public InventoryStockResponse updateStock(UpdateInventoryStockRequest request) {
    Long hospitalId = hospitalContext.requireHospitalId();
    Item item =
        itemRepository
            .findByIdAndHospitalId(request.itemId(), hospitalId)
            .orElseThrow(
                () ->
                    new ApiException(
                        "NOT_FOUND", "Inventory item not found", HttpStatus.NOT_FOUND));

    InventoryStock stock = getOrCreateStock(hospitalId, item);
    stock.setQuantity(stock.getQuantity().add(request.quantity()));
    stock.setLastUpdated(LocalDateTime.now());
    inventoryStockRepository.save(stock);

    recordTransaction(hospitalId, item, request.quantity());

    return toStockResponse(stock);
  }

  private InventoryStock getOrCreateStock(Long hospitalId, Item item) {
    return inventoryStockRepository
        .findByHospitalIdAndItemId(hospitalId, item.getId())
        .orElseGet(
            () -> {
              InventoryStock inventoryStock = new InventoryStock();
              inventoryStock.setHospitalId(hospitalId);
              inventoryStock.setItem(item);
              inventoryStock.setQuantity(BigDecimal.ZERO);
              inventoryStock.setLastUpdated(LocalDateTime.now());
              return inventoryStock;
            });
  }

  private void recordTransaction(Long hospitalId, Item item, BigDecimal quantity) {
    InventoryTransaction transaction = new InventoryTransaction();
    transaction.setHospitalId(hospitalId);
    transaction.setItem(item);
    transaction.setType(InventoryTransactionType.IN);
    transaction.setQuantity(quantity);
    transaction.setReference("MANUAL_STOCK_UPDATE");
    inventoryTransactionRepository.save(transaction);
  }

  @Transactional(readOnly = true)
  public List<InventoryStockResponse> listStock() {
    Long hospitalId = hospitalContext.requireHospitalId();
    return inventoryStockRepository.findAllByHospitalIdOrderByUpdatedAtDesc(hospitalId).stream()
        .map(this::toStockResponse)
        .toList();
  }

  private ItemResponse toItemResponse(Item item) {
    return new ItemResponse(
        item.getId(), item.getName(), item.getCategory(), item.getUnit(), item.getCreatedAt());
  }

  private InventoryStockResponse toStockResponse(InventoryStock stock) {
    return new InventoryStockResponse(
        stock.getItem().getId(),
        stock.getItem().getName(),
        stock.getQuantity(),
        stock.getLastUpdated());
  }
}
