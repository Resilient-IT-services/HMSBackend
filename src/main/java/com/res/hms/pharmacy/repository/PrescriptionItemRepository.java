package com.res.hms.pharmacy.repository;

import com.res.hms.domain.PrescriptionItem;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, UUID> {}
