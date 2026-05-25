package com.res.hms.iam.infrastructure.repository;

import com.res.hms.iam.domain.UserContract;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserContractRepository
    extends JpaRepository<UserContract, UUID>, JpaSpecificationExecutor<UserContract> {

  @Query(
      "SELECT a FROM UserContract a JOIN a.doctorDetails dd JOIN a.user u JOIN u.doctorProfile d "
          + "WHERE d.id = :doctorId AND a.hospitalId = :hospitalId AND a.active = true")
  Optional<UserContract> findByDoctorIdAndHospitalId(
      @Param("doctorId") UUID doctorId, @Param("hospitalId") UUID hospitalId);

  @Query(
      "SELECT a FROM UserContract a JOIN a.nurseDetails nd JOIN a.user u JOIN u.nurseProfile n "
          + "WHERE n.id = :nurseId AND a.hospitalId = :hospitalId AND a.active = true")
  Optional<UserContract> findByNurseIdAndHospitalId(
      @Param("nurseId") UUID nurseId, @Param("hospitalId") UUID hospitalId);

  @Query(
      "SELECT COUNT(a) FROM UserContract a WHERE a.role.code = :roleCode AND a.active = :active AND a.user.active = true")
  long countByRoleCodeAndActive(
      @Param("roleCode") com.res.hms.iam.domain.RoleCode roleCode, @Param("active") boolean active);

    @Query(
        "SELECT COUNT(DISTINCT a.user.id) " + "FROM UserContract a " + "WHERE a.role.code = :roleCode " + "AND a.active = :active " + "AND a.user.active = true")
    long countDistinctUsersByRoleCodeAndActive(
        @Param("roleCode") com.res.hms.iam.domain.RoleCode roleCode, @Param("active") boolean active);

  java.util.List<UserContract> findByHospitalIdAndRoleCodeAndActive(
      UUID hospitalId, com.res.hms.iam.domain.RoleCode roleCode, boolean active);
}
