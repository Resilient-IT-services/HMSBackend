package com.res.hms.clinical.infrastructure.repository;

import com.res.hms.clinical.domain.DoctorSchedule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, UUID> {

  List<DoctorSchedule> findByAssignmentId(UUID assignmentId);

  void deleteByAssignmentId(UUID assignmentId);

  @Query(
      "SELECT s FROM DoctorSchedule s WHERE s.assignmentId = :assignmentId AND s.dayOfWeek = :dayOfWeek")
  List<DoctorSchedule> findByAssignmentAndDay(
      @Param("assignmentId") UUID assignmentId, @Param("dayOfWeek") Short dayOfWeek);
}
