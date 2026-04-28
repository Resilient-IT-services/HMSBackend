package com.res.hms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "doctor_id", nullable = false)
  private DoctorProfile doctorProfile;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hospital_id", nullable = false)
  private Hospital hospital;

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
