package com.res.hms.inventory.infrastructure.repository;

import com.res.hms.core.repository.BaseRepository;
import com.res.hms.inventory.domain.InventoryStock;
import java.util.Optional;
import java.util.UUID;

public interface InventoryStockRepository extends BaseRepository<InventoryStock> {
  Optional<InventoryStock> findByHospitalIdAndItemId(UUID hospitalId, UUID itemId);
}
