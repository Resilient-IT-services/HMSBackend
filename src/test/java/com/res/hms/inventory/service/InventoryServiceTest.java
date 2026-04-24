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
import java.util.UUID;
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
    when(hospitalContext.requireHospitalId()).thenReturn(UUID.nameUUIDFromBytes("4".getBytes()));
    when(itemRepository.save(any(Item.class)))
        .thenAnswer(
            invocation -> {
              Item item = invocation.getArgument(0);
              item.setId(UUID.nameUUIDFromBytes("60".getBytes()));
              return item;
            });

    var response =
        inventoryService.createItem(new CreateItemRequest("Bandage", "Consumable", "pcs"));

    assertEquals("Bandage", response.name());
  }

  @Test
  void listItemsReturnsMappedItems() {
    Item item = new Item();
    item.setId(UUID.nameUUIDFromBytes("1".getBytes()));
    item.setName("Bandage");
    item.setCategory("Consumable");
    item.setUnit("pcs");
    when(hospitalContext.requireHospitalId()).thenReturn(UUID.nameUUIDFromBytes("4".getBytes()));
    when(itemRepository.findAllByHospitalIdOrderByCreatedAtDesc(
            eq(UUID.nameUUIDFromBytes("4".getBytes())), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(item)));

    var response = inventoryService.listItems(0, 10);

    assertEquals(1, response.content().size());
    assertEquals("pcs", response.content().getFirst().unit());
  }

  @Test
  void updateStockRejectsMissingItem() {
    when(hospitalContext.requireHospitalId()).thenReturn(UUID.nameUUIDFromBytes("4".getBytes()));
    when(resourceLookup.findRequired(
            eq(UUID.nameUUIDFromBytes("99".getBytes())),
            eq(UUID.nameUUIDFromBytes("4".getBytes())),
            any(),
            anyString()))
        .thenThrow(new ApiException("NOT_FOUND", "Item not found", HttpStatus.NOT_FOUND));

    ApiException exception =
        assertThrows(
            ApiException.class,
            () ->
                inventoryService.updateStock(
                    new UpdateInventoryStockRequest(
                        UUID.nameUUIDFromBytes("99".getBytes()), BigDecimal.ONE)));

    assertEquals("NOT_FOUND", exception.getCode());
  }

  @Test
  void updateStockCreatesNewStockWhenMissing() {
    Item item = new Item();
    item.setId(UUID.nameUUIDFromBytes("1".getBytes()));
    item.setName("Bandage");
    when(hospitalContext.requireHospitalId()).thenReturn(UUID.nameUUIDFromBytes("4".getBytes()));
    when(resourceLookup.findRequired(
            eq(UUID.nameUUIDFromBytes("1".getBytes())),
            eq(UUID.nameUUIDFromBytes("4".getBytes())),
            any(),
            anyString()))
        .thenReturn(item);
    when(inventoryStockRepository.findByHospitalIdAndItemId(
            UUID.nameUUIDFromBytes("4".getBytes()), UUID.nameUUIDFromBytes("1".getBytes())))
        .thenReturn(Optional.empty());
    when(inventoryStockRepository.save(any(InventoryStock.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response =
        inventoryService.updateStock(
            new UpdateInventoryStockRequest(
                UUID.nameUUIDFromBytes("1".getBytes()), new BigDecimal("5.5")));

    assertEquals(new BigDecimal("5.5"), response.quantity());
    verify(inventoryTransactionRepository).save(any());
  }

  @Test
  void updateStockAddsQuantityToExistingStock() {
    Item item = new Item();
    item.setId(UUID.nameUUIDFromBytes("1".getBytes()));
    item.setName("Bandage");
    InventoryStock stock = new InventoryStock();
    stock.setHospitalId(UUID.nameUUIDFromBytes("4".getBytes()));
    stock.setItem(item);
    stock.setQuantity(new BigDecimal("2.0"));
    when(hospitalContext.requireHospitalId()).thenReturn(UUID.nameUUIDFromBytes("4".getBytes()));
    when(resourceLookup.findRequired(
            eq(UUID.nameUUIDFromBytes("1".getBytes())),
            eq(UUID.nameUUIDFromBytes("4".getBytes())),
            any(),
            anyString()))
        .thenReturn(item);
    when(inventoryStockRepository.findByHospitalIdAndItemId(
            UUID.nameUUIDFromBytes("4".getBytes()), UUID.nameUUIDFromBytes("1".getBytes())))
        .thenReturn(Optional.of(stock));
    when(inventoryStockRepository.save(any(InventoryStock.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response =
        inventoryService.updateStock(
            new UpdateInventoryStockRequest(
                UUID.nameUUIDFromBytes("1".getBytes()), new BigDecimal("3.0")));

    assertEquals(new BigDecimal("5.0"), response.quantity());
  }

  @Test
  void listStockReturnsMappedStock() {
    Item item = new Item();
    item.setId(UUID.nameUUIDFromBytes("1".getBytes()));
    item.setName("Bandage");
    InventoryStock stock = new InventoryStock();
    stock.setItem(item);
    stock.setQuantity(new BigDecimal("5.0"));
    when(hospitalContext.requireHospitalId()).thenReturn(UUID.nameUUIDFromBytes("4".getBytes()));
    when(inventoryStockRepository.findAllByHospitalIdOrderByUpdatedAtDesc(
            UUID.nameUUIDFromBytes("4".getBytes())))
        .thenReturn(List.of(stock));

    var response = inventoryService.listStock();

    assertEquals(1, response.size());
    assertEquals("Bandage", response.getFirst().itemName());
  }
}
