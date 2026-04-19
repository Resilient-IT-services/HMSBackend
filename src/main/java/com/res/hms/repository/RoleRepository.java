package com.res.hms.repository;

import com.res.hms.domain.Role;
import com.res.hms.domain.RoleCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
  Optional<Role> findByCode(RoleCode code);
}
