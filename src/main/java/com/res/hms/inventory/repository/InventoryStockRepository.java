package com.res.hms.inventory.repository;

import com.res.hms.domain.InventoryStock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryStockRepository extends JpaRepository<InventoryStock, UUID> {
  Optional<InventoryStock> findByHospitalIdAndItemId(UUID hospitalId, UUID itemId);

  List<InventoryStock> findAllByHospitalIdOrderByUpdatedAtDesc(UUID hospitalId);
}
