package com.res.hms.nurse.repository;

import com.res.hms.domain.Nurse;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NurseRepository extends JpaRepository<Nurse, Long> {
  Optional<Nurse> findByUserId(Long userId);

  @Query(
      "SELECT n FROM Nurse n JOIN n.user u JOIN u.assignments a "
          + "WHERE a.hospitalId = :hospitalId AND a.role.code = com.res.hms.domain.RoleCode.NURSE "
          + "ORDER BY n.createdAt DESC")
  Page<Nurse> findAllByHospitalIdOrderByCreatedAtDesc(
      @Param("hospitalId") Long hospitalId, Pageable pageable);

  @Query(
      "SELECT n FROM Nurse n JOIN n.user u JOIN u.assignments a "
          + "WHERE n.id = :id AND a.hospitalId = :hospitalId AND a.role.code = com.res.hms.domain.RoleCode.NURSE")
  Optional<Nurse> findByIdAndHospitalId(@Param("id") Long id, @Param("hospitalId") Long hospitalId);
}
