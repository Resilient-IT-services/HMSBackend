package com.res.hms.doctor.repository;

import com.res.hms.domain.DoctorProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, UUID> {
  Optional<DoctorProfile> findByUserId(UUID userId);

  boolean existsByLicenseNumber(String licenseNumber);

  boolean existsByLicenseNumberAndIdNot(String licenseNumber, UUID id);

  @Query(
      "SELECT COUNT(d) > 0 FROM DoctorProfile d WHERE d.licenseNumber = :licenseNumber AND d.user.id <> :userId")
  boolean existsByLicenseNumberAndUserIdNot(
      @Param("licenseNumber") String licenseNumber, @Param("userId") UUID userId);

  @Query(
      "SELECT d FROM DoctorProfile d JOIN d.user u JOIN u.assignments a "
          + "WHERE a.hospitalId = :hospitalId AND a.role.code = com.res.hms.domain.RoleCode.DOCTOR")
  Page<DoctorProfile> findAllByHospitalId(@Param("hospitalId") UUID hospitalId, Pageable pageable);

  @Query(
      "SELECT d FROM DoctorProfile d JOIN d.user u JOIN u.assignments a "
          + "WHERE a.hospitalId = :hospitalId AND a.role.code = com.res.hms.domain.RoleCode.DOCTOR "
          + "AND (LOWER(u.fullName) LIKE :query OR "
          + "LOWER(d.specialization) LIKE :query)")
  Page<DoctorProfile> search(
      @Param("hospitalId") UUID hospitalId, @Param("query") String query, Pageable pageable);

  Optional<DoctorProfile> findByIdAndUserId(UUID id, UUID userId);

  @Query(
      "SELECT d FROM DoctorProfile d JOIN d.user u JOIN u.assignments a "
          + "WHERE d.id = :id AND a.hospitalId = :hospitalId AND a.role.code = com.res.hms.domain.RoleCode.DOCTOR")
  Optional<DoctorProfile> findByIdAndHospitalId(
      @Param("id") UUID id, @Param("hospitalId") UUID hospitalId);
}
