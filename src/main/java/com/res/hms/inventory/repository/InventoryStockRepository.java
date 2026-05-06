package com.res.hms.inventory.repository;

import com.res.hms.common.repository.BaseRepository;
import com.res.hms.domain.InventoryStock;
import java.util.Optional;
import java.util.UUID;

public interface InventoryStockRepository extends BaseRepository<InventoryStock> {
  Optional<InventoryStock> findByHospitalIdAndItemId(UUID hospitalId, UUID itemId);
}
