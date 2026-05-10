package com.res.hms.iam.infrastructure.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.res.hms.iam.domain.Role;
import com.res.hms.iam.domain.RoleCode;

public interface RoleRepository extends JpaRepository<Role, UUID> {
  Optional<Role> findByCode(RoleCode code);
}
