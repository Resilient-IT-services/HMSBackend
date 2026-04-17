package com.res.hms.inventory.repository;

import com.res.hms.domain.InventoryStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryStockRepository extends JpaRepository<InventoryStock, Long> {
    Optional<InventoryStock> findByHospitalIdAndItemId(Long hospitalId, Long itemId);
    List<InventoryStock> findAllByHospitalIdOrderByUpdatedAtDesc(Long hospitalId);
}
