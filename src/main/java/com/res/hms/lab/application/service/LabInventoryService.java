package com.res.hms.lab.application.service;

import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.lab.application.dto.LabInventoryItemDTO;
import com.res.hms.lab.domain.LabInventoryItem;
import com.res.hms.lab.domain.LabInventoryStock;
import com.res.hms.lab.domain.LabInventoryTransaction;
import com.res.hms.lab.infrastructure.repository.LabInventoryItemRepository;
import com.res.hms.lab.infrastructure.repository.LabInventoryStockRepository;
import com.res.hms.lab.infrastructure.repository.LabInventoryTransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabInventoryService {

  private final LabInventoryItemRepository itemRepository;
  private final LabInventoryStockRepository stockRepository;
  private final LabInventoryTransactionRepository transactionRepository;
  private final HospitalContext hospitalContext;
  private final LabMapper mapper;

  @Transactional(readOnly = true)
  public List<LabInventoryItemDTO> listInventory() {
    UUID hospitalId = hospitalContext.requireHospitalId();
    List<LabInventoryItem> items = itemRepository.findAll();

    return items.stream()
        .map(
            item -> {
              LabInventoryItemDTO dto = mapper.toDto(item);
              stockRepository.findAll().stream()
                  .filter(
                      s ->
                          s.getItemId().equals(item.getId())
                              && s.getHospitalId().equals(hospitalId))
                  .findFirst()
                  .ifPresent(
                      s -> {
                        dto.setAvailableQty(s.getAvailableQty());
                        dto.setReorderLevel(s.getReorderLevel());
                      });
              return dto;
            })
        .collect(Collectors.toList());
  }

  @Transactional
  public LabInventoryItemDTO createItem(LabInventoryItemDTO dto) {
    LabInventoryItem entity = mapper.toEntity(dto);
    entity = itemRepository.save(entity);

    UUID hospitalId = hospitalContext.requireHospitalId();
    LabInventoryStock stock =
        LabInventoryStock.builder()
            .itemId(entity.getId())
            .hospitalId(hospitalId)
            .availableQty(BigDecimal.ZERO)
            .reorderLevel(dto.getReorderLevel() != null ? dto.getReorderLevel() : BigDecimal.TEN)
            .build();
    stockRepository.save(stock);

    return mapper.toDto(entity);
  }

  @Transactional
  public void recordTransaction(UUID itemId, BigDecimal quantity, String type) {
    UUID hospitalId = hospitalContext.requireHospitalId();

    LabInventoryStock stock =
        stockRepository.findAll().stream()
            .filter(s -> s.getItemId().equals(itemId) && s.getHospitalId().equals(hospitalId))
            .findFirst()
            .orElseGet(
                () ->
                    stockRepository.save(
                        LabInventoryStock.builder()
                            .itemId(itemId)
                            .hospitalId(hospitalId)
                            .availableQty(BigDecimal.ZERO)
                            .reorderLevel(BigDecimal.TEN)
                            .build()));

    if ("ADD".equalsIgnoreCase(type)) {
      stock.setAvailableQty(stock.getAvailableQty().add(quantity));
    } else if ("REMOVE".equalsIgnoreCase(type)) {
      stock.setAvailableQty(stock.getAvailableQty().subtract(quantity));
    }

    stockRepository.save(stock);

    transactionRepository.save(
        LabInventoryTransaction.builder()
            .itemId(itemId)
            .transactionType(type)
            .quantity(quantity)
            .transactionTime(Instant.now())
            .build());
  }
}
