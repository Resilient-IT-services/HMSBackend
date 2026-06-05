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
@Table(name = "lab_sample_rejection", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabSampleRejection {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "sample_id")
  private UUID sampleId;

  @Column(name = "reason")
  private String reason;

  @Column(name = "rejected_by")
  private UUID rejectedBy;

  @Column(name = "rejected_at")
  private Instant rejectedAt;
}
