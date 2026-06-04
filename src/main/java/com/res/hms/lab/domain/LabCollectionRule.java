package com.res.hms.lab.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "lab_collection_rule", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabCollectionRule {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "test_id")
  private UUID testId;

  @Column(name = "barcode_required")
  private Boolean barcodeRequired;

  @Column(name = "home_collection_allowed")
  private Boolean homeCollectionAllowed;

  @Column(name = "collection_mandatory")
  private Boolean collectionMandatory;
}
