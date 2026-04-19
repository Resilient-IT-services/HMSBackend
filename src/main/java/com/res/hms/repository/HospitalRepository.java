package com.res.hms.repository;

import com.res.hms.domain.Hospital;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {
  Optional<Hospital> findByCode(String code);
}
