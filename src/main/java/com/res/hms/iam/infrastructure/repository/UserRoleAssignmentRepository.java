package com.res.hms.iam.infrastructure.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.res.hms.iam.domain.UserRoleAssignment;

@Repository
public interface UserRoleAssignmentRepository
    extends JpaRepository<UserRoleAssignment, UUID>, JpaSpecificationExecutor<UserRoleAssignment> {}
