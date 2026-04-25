package com.res.hms.nurse.repository;

import com.res.hms.domain.Nurse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NurseRepository extends JpaRepository<Nurse, UUID> {
  Optional<Nurse> findByUserId(UUID userId);

  boolean existsByNursingLicenseNo(String nursingLicenseNo);

  boolean existsByNursingLicenseNoAndIdNot(String nursingLicenseNo, UUID id);

  @Query("SELECT COUNT(n) > 0 FROM Nurse n WHERE n.nursingLicenseNo = :nursingLicenseNo AND n.user.id <> :userId")
  boolean existsByNursingLicenseNoAndUserIdNot(@Param("nursingLicenseNo") String nursingLicenseNo, @Param("userId") UUID userId);

  @Query(
      "SELECT n FROM Nurse n JOIN n.user u JOIN u.assignments a "
          + "WHERE a.hospitalId = :hospitalId AND a.role.code = com.res.hms.domain.RoleCode.NURSE "
          + "ORDER BY n.createdAt DESC")
  Page<Nurse> findAllByHospitalIdOrderByCreatedAtDesc(
      @Param("hospitalId") UUID hospitalId, Pageable pageable);

  @Query(
      "SELECT n FROM Nurse n JOIN n.user u JOIN u.assignments a "
          + "WHERE n.id = :id AND a.hospitalId = :hospitalId AND a.role.code = com.res.hms.domain.RoleCode.NURSE")
  Optional<Nurse> findByIdAndHospitalId(@Param("id") UUID id, @Param("hospitalId") UUID hospitalId);
}
