package com.res.hms.hospital.infrastructure.repository;

import com.res.hms.hospital.domain.Department;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {
  Optional<Department> findByName(String name);
}
