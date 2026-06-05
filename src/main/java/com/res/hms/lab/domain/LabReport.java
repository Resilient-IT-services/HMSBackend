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
@Table(name = "lab_report", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabReport {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "order_id")
  private UUID orderId;

  @Column(name = "report_number")
  private String reportNumber;

  @Column(name = "generated_at")
  private Instant generatedAt;

  @Column(name = "pdf_path")
  private String pdfPath;
}
