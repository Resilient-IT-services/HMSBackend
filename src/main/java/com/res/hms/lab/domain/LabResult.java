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
@Table(name = "lab_result", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabResult {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "order_test_id")
  private UUID orderTestId;

  @Column(name = "sample_id")
  private UUID sampleId;

  @Column(name = "entered_by")
  private UUID enteredBy;

  @Column(name = "entered_at")
  private Instant enteredAt;

  @Column(name = "findings", columnDefinition = "TEXT")
  private String findings;
}
