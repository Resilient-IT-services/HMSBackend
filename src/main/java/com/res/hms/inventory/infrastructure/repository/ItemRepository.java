package com.res.hms.inventory.infrastructure.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.res.hms.core.repository.BaseRepository;
import com.res.hms.inventory.domain.Item;

public interface ItemRepository extends BaseRepository<Item> {
  Page<Item> findAllByHospitalIdAndNameContainingIgnoreCase(
      UUID hospitalId, String name, Pageable pageable);
}
