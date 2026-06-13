package com.res.hms.lab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lab_test", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabTest {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "test_code")
  private String testCode;

  @Column(name = "test_name")
  private String testName;

  @Column(name = "short_name")
  private String shortName;

  @Column(name = "alias_name")
  private String aliasName;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id", insertable = false, updatable = false)
  private LabDepartment department;

  @Column(name = "department_id")
  private UUID departmentId;

  @Enumerated(EnumType.STRING)
  @Column(name = "category")
  private TestCategory category;

  @Enumerated(EnumType.STRING)
  @Column(name = "result_entry_type")
  private ResultEntryType resultEntryType;

  @Column(name = "tat_value")
  private Integer tatValue;

  @Column(name = "tat_unit")
  private String tatUnit;

  @Column(name = "active")
  private Boolean active;

  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;
}
