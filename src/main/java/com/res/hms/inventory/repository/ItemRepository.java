package com.res.hms.inventory.repository;

import com.res.hms.domain.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {
    Page<Item> findAllByHospitalIdOrderByCreatedAtDesc(Long hospitalId, Pageable pageable);
    Optional<Item> findByIdAndHospitalId(Long id, Long hospitalId);
}
