package com.res.hms.lab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lab_order", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "hospital_id")
  private UUID hospitalId;

  @Column(name = "patient_id")
  private UUID patientId;

  @Column(name = "encounter_id")
  private UUID encounterId;

  @Column(name = "doctor_id")
  private UUID doctorId;

  @Column(name = "order_number")
  private String orderNumber;

  @Column(name = "status")
  private String status;

  @Column(name = "ordered_at")
  private Instant orderedAt;

  @Column(name = "order_source")
  private String orderSource;
}
