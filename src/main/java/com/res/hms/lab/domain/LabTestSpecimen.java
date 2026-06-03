package com.res.hms.lab.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "lab_test_specimen", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabTestSpecimen {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "test_id")
  private UUID testId;

  @Column(name = "specimen_type_id")
  private UUID specimenTypeId;

  @Column(name = "container_type")
  private String containerType;

  @Column(name = "volume_required")
  private BigDecimal volumeRequired;

  @Column(name = "fasting_required")
  private Boolean fastingRequired;

  @Column(name = "collection_instruction")
  private String collectionInstruction;
}
