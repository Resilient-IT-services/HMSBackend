package com.res.hms.inventory.repository;

import com.res.hms.domain.InventoryStock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryStockRepository extends JpaRepository<InventoryStock, Long> {
  Optional<InventoryStock> findByHospitalIdAndItemId(Long hospitalId, Long itemId);

  List<InventoryStock> findAllByHospitalIdOrderByUpdatedAtDesc(Long hospitalId);
}
