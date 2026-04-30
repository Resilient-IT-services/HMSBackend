package com.res.hms.repository;

import com.res.hms.common.repository.BaseRepository;
import com.res.hms.domain.Item;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemRepository extends BaseRepository<Item> {
  Page<Item> findAllByHospitalIdAndNameContainingIgnoreCase(
      UUID hospitalId, String name, Pageable pageable);
}
