package com.res.hms.inventory.service;

import com.res.hms.common.api.PageResponse;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.domain.InventoryStock;
import com.res.hms.domain.InventoryTransaction;
import com.res.hms.domain.InventoryTransactionType;
import com.res.hms.domain.Item;
import com.res.hms.inventory.dto.InventoryStockDTO;
import com.res.hms.inventory.dto.ItemDTO;
import com.res.hms.inventory.dto.StockTransactionRequest;
import com.res.hms.inventory.repository.InventoryStockRepository;
import com.res.hms.inventory.repository.InventoryTransactionRepository;
import com.res.hms.inventory.repository.ItemRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.res.hms.inventory.mapper.InventoryMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

  private final ItemRepository itemRepository;
  private final InventoryStockRepository inventoryStockRepository;
  private final InventoryTransactionRepository inventoryTransactionRepository;
  private final HospitalContext hospitalContext;
  private final ResourceLookup resourceLookup;
  private final InventoryMapper inventoryMapper;

  public PageResponse<ItemDTO> listItems(int page, int size, String search) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    PageRequest pageRequest = PageRequest.of(page, size);

    if (search != null && !search.trim().isEmpty()) {
      return PageResponse.from(
          itemRepository
              .findAllByHospitalIdAndNameContainingIgnoreCase(hospitalId, search, pageRequest)
              .map(inventoryMapper::toDto));
    }

    return PageResponse.from(
        itemRepository
            .findAllByHospitalIdOrderByCreatedAtDesc(hospitalId, pageRequest)
            .map(inventoryMapper::toDto));
  }

  @Transactional
  public ItemDTO createItem(ItemDTO dto) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    Item item = inventoryMapper.toEntity(dto);
    item.setHospitalId(hospitalId);
    item = itemRepository.save(item);
    return inventoryMapper.toDto(item);
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
      newQuantity = stock.getQuantity().add(request.getQuantity());
    }

    stock.setQuantity(newQuantity);
    stock.setLastUpdated(LocalDateTime.now());
    stock = inventoryStockRepository.save(stock);

    InventoryStockDTO dto = inventoryMapper.toDto(stock);
    dto.setItemName(item.getName());
    return dto;
  }

  public ItemDTO toDto(Item item) {
    return inventoryMapper.toDto(item);
  }
}
