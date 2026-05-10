package com.res.hms.inventory.application.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.res.hms.core.api.PageResponse;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.core.util.ResourceLookup;
import com.res.hms.inventory.application.dto.InventoryStockDTO;
import com.res.hms.inventory.application.dto.ItemDTO;
import com.res.hms.inventory.application.dto.StockTransactionRequest;
import com.res.hms.inventory.domain.InventoryStock;
import com.res.hms.inventory.domain.InventoryTransaction;
import com.res.hms.inventory.domain.InventoryTransactionType;
import com.res.hms.inventory.domain.Item;
import com.res.hms.inventory.infrastructure.repository.InventoryStockRepository;
import com.res.hms.inventory.infrastructure.repository.InventoryTransactionRepository;
import com.res.hms.inventory.infrastructure.repository.ItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

  private final ItemRepository itemRepository;
  private final InventoryStockRepository inventoryStockRepository;
  private final InventoryTransactionRepository inventoryTransactionRepository;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;

  public PageResponse<ItemDTO> listItems(int page, int size, String search) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    PageRequest pageRequest = PageRequest.of(page, size);

    if (search != null && !search.trim().isEmpty()) {
      return PageResponse.from(
          itemRepository
              .findAllByHospitalIdAndNameContainingIgnoreCase(hospitalId, search, pageRequest)
              .map(this::toDto));
    }

    return PageResponse.from(
        itemRepository
            .findAllByHospitalIdOrderByCreatedAtDesc(hospitalId, pageRequest)
            .map(this::toDto));
  }

  @Transactional
  public ItemDTO createItem(ItemDTO dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Item item = new Item();
    item.setHospitalId(hospitalId);
    item.setName(dto.getName());
    item.setCategory(dto.getCategory());
    item.setUnit(dto.getUnit());
    item = itemRepository.save(item);
    return toDto(item);
  }

  @Transactional
  public InventoryStockDTO recordTransaction(StockTransactionRequest request) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Item item =
        resourceLookup.findRequired(
            request.getItemId(), hospitalId, itemRepository::findByIdAndHospitalId, "Item");

    InventoryTransaction transaction = new InventoryTransaction();
    transaction.setHospitalId(hospitalId);
    transaction.setItemId(item.getId());
    transaction.setType(request.getType());
    transaction.setQuantity(request.getQuantity());
    transaction.setReference(request.getReference());
    inventoryTransactionRepository.save(transaction);

    InventoryStock stock =
        inventoryStockRepository
            .findByHospitalIdAndItemId(hospitalId, item.getId())
            .orElseGet(
                () -> {
                  InventoryStock newStock = new InventoryStock();
                  newStock.setHospitalId(hospitalId);
                  newStock.setItemId(item.getId());
                  newStock.setQuantity(BigDecimal.ZERO);
                  return newStock;
                });

    BigDecimal newQuantity;
    if (request.getType() == InventoryTransactionType.STOCK_IN
        || request.getType() == InventoryTransactionType.RETURN) {
      newQuantity = stock.getQuantity().add(request.getQuantity());
    } else if (request.getType() == InventoryTransactionType.STOCK_OUT) {
      newQuantity = stock.getQuantity().subtract(request.getQuantity());
    } else { // ADJUSTMENT
      // For ADJUSTMENT, we might want a different logic, but let's assume it's a delta
      newQuantity = stock.getQuantity().add(request.getQuantity());
    }

    stock.setQuantity(newQuantity);
    stock.setLastUpdated(LocalDateTime.now());
    stock = inventoryStockRepository.save(stock);

    return InventoryStockDTO.builder()
        .itemId(item.getId())
        .itemName(item.getName())
        .quantity(stock.getQuantity())
        .lastUpdated(stock.getLastUpdated())
        .build();
  }

  public ItemDTO toDto(Item item) {
    return ItemDTO.builder()
        .id(item.getId())
        .name(item.getName())
        .category(item.getCategory())
        .unit(item.getUnit())
        .build();
  }
}
