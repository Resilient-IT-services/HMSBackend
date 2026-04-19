package com.res.hms.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.res.hms.common.exception.ApiException;
import com.res.hms.common.tenant.HospitalContext;
import com.res.hms.common.util.ResourceLookup;
import com.res.hms.domain.InventoryStock;
import com.res.hms.domain.Item;
import com.res.hms.inventory.dto.CreateItemRequest;
import com.res.hms.inventory.dto.UpdateInventoryStockRequest;
import com.res.hms.inventory.repository.InventoryStockRepository;
import com.res.hms.inventory.repository.InventoryTransactionRepository;
import com.res.hms.inventory.repository.ItemRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

  @Mock private ItemRepository itemRepository;
  @Mock private InventoryStockRepository inventoryStockRepository;
  @Mock private InventoryTransactionRepository inventoryTransactionRepository;
  @Mock private HospitalContext hospitalContext;
  @Mock private ResourceLookup resourceLookup;

  @InjectMocks private InventoryService inventoryService;

  @Test
  void createItemPersistsItem() {
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(itemRepository.save(any(Item.class)))
        .thenAnswer(
            invocation -> {
              Item item = invocation.getArgument(0);
              item.setId(60L);
              return item;
            });

    var response =
        inventoryService.createItem(new CreateItemRequest("Bandage", "Consumable", "pcs"));

    assertEquals("Bandage", response.name());
  }

  @Test
  void listItemsReturnsMappedItems() {
    Item item = new Item();
    item.setId(1L);
    item.setName("Bandage");
    item.setCategory("Consumable");
    item.setUnit("pcs");
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(itemRepository.findAllByHospitalIdOrderByCreatedAtDesc(eq(4L), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(item)));

    var response = inventoryService.listItems(0, 10);

    assertEquals(1, response.content().size());
    assertEquals("pcs", response.content().getFirst().unit());
  }

  @Test
  void updateStockRejectsMissingItem() {
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(resourceLookup.findRequired(eq(99L), eq(4L), any(), anyString()))
        .thenThrow(new ApiException("NOT_FOUND", "Item not found", HttpStatus.NOT_FOUND));

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                inventoryService.updateStock(new UpdateInventoryStockRequest(99L, BigDecimal.ONE)));

    assertEquals("NOT_FOUND", exception.getCode());
  }

  @Test
  void updateStockCreatesNewStockWhenMissing() {
    Item item = new Item();
    item.setId(1L);
    item.setName("Bandage");
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(resourceLookup.findRequired(eq(1L), eq(4L), any(), anyString())).thenReturn(item);
    when(inventoryStockRepository.findByHospitalIdAndItemId(4L, 1L)).thenReturn(Optional.empty());
    when(inventoryStockRepository.save(any(InventoryStock.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response =
        inventoryService.updateStock(new UpdateInventoryStockRequest(1L, new BigDecimal("5.5")));

    assertEquals(new BigDecimal("5.5"), response.quantity());
    verify(inventoryTransactionRepository).save(any());
  }

  @Test
  void updateStockAddsQuantityToExistingStock() {
    Item item = new Item();
    item.setId(1L);
    item.setName("Bandage");
    InventoryStock stock = new InventoryStock();
    stock.setHospitalId(4L);
    stock.setItem(item);
    stock.setQuantity(new BigDecimal("2.0"));
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(resourceLookup.findRequired(eq(1L), eq(4L), any(), anyString())).thenReturn(item);
    when(inventoryStockRepository.findByHospitalIdAndItemId(4L, 1L)).thenReturn(Optional.of(stock));
    when(inventoryStockRepository.save(any(InventoryStock.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response =
        inventoryService.updateStock(new UpdateInventoryStockRequest(1L, new BigDecimal("3.0")));

    assertEquals(new BigDecimal("5.0"), response.quantity());
  }

  @Test
  void listStockReturnsMappedStock() {
    Item item = new Item();
    item.setId(1L);
    item.setName("Bandage");
    InventoryStock stock = new InventoryStock();
    stock.setItem(item);
    stock.setQuantity(new BigDecimal("5.0"));
    when(hospitalContext.requireHospitalId()).thenReturn(4L);
    when(inventoryStockRepository.findAllByHospitalIdOrderByUpdatedAtDesc(4L))
        .thenReturn(List.of(stock));

    var response = inventoryService.listStock();

    assertEquals(1, response.size());
    assertEquals("Bandage", response.getFirst().itemName());
  }
}
