package com.res.hms.inventory.application.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.inventory.infrastructure.repository.ItemRepository;
import com.res.hms.inventory.infrastructure.repository.InventoryStockRepository;
import com.res.hms.inventory.infrastructure.repository.InventoryTransactionRepository;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock private ItemRepository itemRepository;
    @Mock private InventoryStockRepository stockRepository;
    @Mock private InventoryTransactionRepository transactionRepository;
    @Mock private HospitalContext hospitalContext;

    @InjectMocks
    private InventoryService inventoryService;

    private UUID hospitalId;

    @BeforeEach
    void setUp() {
        hospitalId = UUID.randomUUID();
        when(hospitalContext.requireHospitalId()).thenReturn(hospitalId);
    }

    @Test
    void testListItems_WithSearch() {
        when(itemRepository.findAllByHospitalIdAndNameContainingIgnoreCase(eq(hospitalId), anyString(), any()))
            .thenReturn(new PageImpl<>(java.util.List.of()));
        
        var result = inventoryService.listItems(0, 10, "test");
        
        assertNotNull(result);
        verify(itemRepository).findAllByHospitalIdAndNameContainingIgnoreCase(eq(hospitalId), eq("test"), any());
    }

    @Test
    void testListItems_WithoutSearch() {
        when(itemRepository.findAllByHospitalIdOrderByCreatedAtDesc(eq(hospitalId), any()))
            .thenReturn(new PageImpl<>(java.util.List.of()));
        
        var result = inventoryService.listItems(0, 10, null);
        
        assertNotNull(result);
        verify(itemRepository).findAllByHospitalIdOrderByCreatedAtDesc(eq(hospitalId), any());
    }
}
