package com.res.hms.lab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lab_order_test", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabOrderTest {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "order_id")
  private UUID orderId;

  @Column(name = "test_id")
  private UUID testId;

  @Column(name = "test_name")
  private String testName;

  @Enumerated(EnumType.STRING)
  @Column(name = "category")
  private TestCategory category;

  @Enumerated(EnumType.STRING)
  @Column(name = "result_entry_type")
  private ResultEntryType resultEntryType;

  @Column(name = "package_id")
  private UUID packageId;

  @Column(name = "status")
  private String status;
}
