package com.res.hms.inventory.application.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.inventory.infrastructure.repository.InventoryStockRepository;
import com.res.hms.inventory.infrastructure.repository.InventoryTransactionRepository;
import com.res.hms.inventory.infrastructure.repository.ItemRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

  @Mock private ItemRepository itemRepository;
  @Mock private InventoryStockRepository stockRepository;
  @Mock private InventoryTransactionRepository transactionRepository;
  @Mock private HospitalContext hospitalContext;

  @InjectMocks private InventoryService inventoryService;

  private UUID hospitalId;

  @BeforeEach
  void setUp() {
    hospitalId = UUID.randomUUID();
    when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
  }

  @Test
  void testListItems() {
    when(itemRepository.findAll(
            any(org.springframework.data.jpa.domain.Specification.class),
            any(org.springframework.data.domain.Pageable.class)))
        .thenReturn(new PageImpl<>(java.util.List.of()));

    var result =
        inventoryService.listItems(
            org.springframework.data.domain.PageRequest.of(0, 10), "test", java.util.Map.of());

    assertNotNull(result);
    verify(itemRepository)
        .findAll(
            any(org.springframework.data.jpa.domain.Specification.class),
            any(org.springframework.data.domain.Pageable.class));
  }
}
