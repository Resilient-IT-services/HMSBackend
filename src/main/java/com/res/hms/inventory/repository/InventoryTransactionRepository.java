package com.res.hms.inventory.repository;

import com.res.hms.domain.InventoryTransaction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {}
