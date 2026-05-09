package com.res.hms.iam.infrastructure.repository;

import com.res.hms.iam.domain.Role;
import com.res.hms.iam.domain.RoleCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {
  Optional<Role> findByCode(RoleCode code);
}
