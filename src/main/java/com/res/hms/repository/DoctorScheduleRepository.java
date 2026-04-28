package com.res.hms.repository;

import com.res.hms.domain.DoctorSchedule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, UUID> {
  @Query(
      "SELECT s FROM DoctorSchedule s WHERE s.assignment.user.doctorProfile.id = :doctorId AND s.assignment.hospitalId = :hospitalId AND s.dayOfWeek = :dayOfWeek")
  List<DoctorSchedule> findByDoctorAndHospitalAndDay(
      @Param("doctorId") UUID doctorId,
      @Param("hospitalId") UUID hospitalId,
      @Param("dayOfWeek") Short dayOfWeek);
}
