package com.res.hms.user.repository;

import com.res.hms.domain.UserRoleAssignment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleAssignmentRepository
    extends JpaRepository<UserRoleAssignment, UUID>, JpaSpecificationExecutor<UserRoleAssignment> {}
