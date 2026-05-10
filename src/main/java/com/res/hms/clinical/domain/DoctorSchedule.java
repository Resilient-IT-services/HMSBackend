package com.res.hms.clinical.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "doctor_schedules")
public class DoctorSchedule {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "assignment_id", nullable = false)
  private java.util.UUID assignmentId;

  @Column(name = "day_of_week", nullable = false)
  private Short dayOfWeek; // 1-7 (Monday-Sunday) to match DB check constraint

  @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  @Column(name = "slot_duration_minutes")
  private Integer slotDurationMinutes = 15;

  @Column(name = "max_patients")
  private Integer maxPatients;
}
