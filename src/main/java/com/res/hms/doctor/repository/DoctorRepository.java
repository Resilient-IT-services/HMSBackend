package com.res.hms.doctor.repository;

import com.res.hms.domain.Doctor;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
  Optional<Doctor> findByUserId(Long userId);

  @Query(
      "SELECT d FROM Doctor d JOIN d.user u JOIN u.assignments a "
          + "WHERE a.hospitalId = :hospitalId AND a.role.code = com.res.hms.domain.RoleCode.DOCTOR "
          + "ORDER BY d.createdAt DESC")
  Page<Doctor> findAllByHospitalIdOrderByCreatedAtDesc(
      @Param("hospitalId") Long hospitalId, Pageable pageable);

  @Query(
      "SELECT d FROM Doctor d JOIN d.user u JOIN u.assignments a "
          + "WHERE a.hospitalId = :hospitalId AND a.role.code = com.res.hms.domain.RoleCode.DOCTOR "
          + "AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR "
          + "LOWER(d.specialization) LIKE LOWER(CONCAT('%', :query, '%'))) "
          + "ORDER BY d.createdAt DESC")
  Page<Doctor> search(
      @Param("hospitalId") Long hospitalId, @Param("query") String query, Pageable pageable);


  Optional<Doctor> findByIdAndUserId(Long id, Long userId);

  @Query(
      "SELECT d FROM Doctor d JOIN d.user u JOIN u.assignments a "
          + "WHERE d.id = :id AND a.hospitalId = :hospitalId AND a.role.code = com.res.hms.domain.RoleCode.DOCTOR")
  Optional<Doctor> findByIdAndHospitalId(
      @Param("id") Long id, @Param("hospitalId") Long hospitalId);
}
