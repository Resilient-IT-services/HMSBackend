package com.res.hms.common.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseRepository<T, K> extends JpaRepository<T, K> {
  Page<T> findAllByHospitalIdOrderByCreatedAtDesc(Long hospitalId, Pageable pageable);

  List<T> findAllByHospitalId(Long hospitalId);

  Optional<T> findByIdAndHospitalId(K id, Long hospitalId);
}
