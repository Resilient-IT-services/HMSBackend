package com.res.hms.iam.infrastructure.repository;

import com.res.hms.iam.domain.UserRoleAssignment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleAssignmentRepository
    extends JpaRepository<UserRoleAssignment, UUID>, JpaSpecificationExecutor<UserRoleAssignment> {

  @Query(
      "SELECT a FROM UserRoleAssignment a JOIN a.user u JOIN u.doctorProfile d "
          + "WHERE d.id = :doctorId AND a.hospitalId = :hospitalId AND a.active = true")
  Optional<UserRoleAssignment> findByDoctorIdAndHospitalId(
      @Param("doctorId") UUID doctorId, @Param("hospitalId") UUID hospitalId);

  @Query(
      "SELECT a FROM UserRoleAssignment a JOIN a.user u JOIN u.nurseProfile n "
          + "WHERE n.id = :nurseId AND a.hospitalId = :hospitalId AND a.active = true")
  Optional<UserRoleAssignment> findByNurseIdAndHospitalId(
      @Param("nurseId") UUID nurseId, @Param("hospitalId") UUID hospitalId);
}
