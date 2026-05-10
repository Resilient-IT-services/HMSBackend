package com.res.hms.clinical.domain;

import com.res.hms.core.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "doctor_availability_exception")
public class DoctorAvailabilityException extends BaseEntity {

  @Column(name = "doctor_id", nullable = false)
  private java.util.UUID doctorId;

  @Column(name = "hospital_id", nullable = false)
  private java.util.UUID hospitalId;

  @Column(name = "exception_date", nullable = false)
  private LocalDate date;

  @Column(name = "type", nullable = false)
  private String type; // FULL_DAY_OFF, MODIFIED, ADDED

  @Column(name = "start_time")
  private LocalTime startTime;

  @Column(name = "end_time")
  private LocalTime endTime;

  @Column(name = "mode")
  private String mode;

  @Column(name = "slot_duration_minutes")
  private Integer slotDurationMinutes;

  @Column(name = "note")
  private String note;
}
