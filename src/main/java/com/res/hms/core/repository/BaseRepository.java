package com.res.hms.core.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseRepository<T> extends JpaRepository<T, UUID> {
  Page<T> findAllByHospitalIdOrderByCreatedAtDesc(UUID hospitalId, Pageable pageable);

  List<T> findAllByHospitalId(UUID hospitalId);

  Optional<T> findByIdAndHospitalId(UUID id, UUID hospitalId);
}
