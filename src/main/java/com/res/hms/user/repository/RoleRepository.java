package com.res.hms.user.repository;

import com.res.hms.domain.Role;
import com.res.hms.domain.RoleCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {
  Optional<Role> findByCode(RoleCode code);
}
