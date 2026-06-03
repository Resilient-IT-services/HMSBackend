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
@Table(name = "lab_sample", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabSample {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "hospital_id")
  private UUID hospitalId;

  @Column(name = "order_test_id")
  private UUID orderTestId;

  @Column(name = "sample_number")
  private String sampleNumber;

  @Column(name = "specimen_type_id")
  private UUID specimenTypeId;

  @Column(name = "collected_at")
  private Instant collectedAt;

  @Column(name = "collected_by")
  private UUID collectedBy;

  @Column(name = "status")
  private String status;
}
