package com.res.hms.iam.infrastructure.repository;

import com.res.hms.iam.domain.NurseProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NurseProfileRepository extends JpaRepository<NurseProfile, UUID> {
  Optional<NurseProfile> findByUserId(UUID userId);

  boolean existsByNursingLicenseNo(String nursingLicenseNo);

  boolean existsByNursingLicenseNoAndIdNot(String nursingLicenseNo, UUID id);

  @Query(
      "SELECT COUNT(n) > 0 FROM NurseProfile n WHERE n.nursingLicenseNo = :nursingLicenseNo AND n.user.id <> :userId")
  boolean existsByNursingLicenseNoAndUserIdNot(
      @Param("nursingLicenseNo") String nursingLicenseNo, @Param("userId") UUID userId);

  @Query(
      "SELECT n FROM NurseProfile n JOIN n.user u JOIN u.assignments a "
          + "WHERE a.hospitalId = :hospitalId AND a.role.code = com.res.hms.iam.domain.RoleCode.NURSE")
  Page<NurseProfile> findAllByHospitalId(@Param("hospitalId") UUID hospitalId, Pageable pageable);

  @Query(
      "SELECT n FROM NurseProfile n JOIN n.user u JOIN u.assignments a "
          + "WHERE n.id = :id AND a.hospitalId = :hospitalId AND a.role.code = com.res.hms.iam.domain.RoleCode.NURSE")
  Optional<NurseProfile> findByIdAndHospitalId(
      @Param("id") UUID id, @Param("hospitalId") UUID hospitalId);
}
