package com.res.hms.inventory.infrastructure.repository;

import java.util.Optional;
import java.util.UUID;

import com.res.hms.core.repository.BaseRepository;
import com.res.hms.inventory.domain.InventoryStock;

public interface InventoryStockRepository extends BaseRepository<InventoryStock> {
  Optional<InventoryStock> findByHospitalIdAndItemId(UUID hospitalId, UUID itemId);
}
