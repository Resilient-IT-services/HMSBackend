package com.res.hms.iam.infrastructure.repository;

import com.res.hms.iam.domain.Specialization;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpecializationRepository extends JpaRepository<Specialization, UUID> {
  Optional<Specialization> findByName(String name);
}
