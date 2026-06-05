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
@Table(name = "lab_report_delivery", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabReportDelivery {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "report_id")
  private UUID reportId;

  @Column(name = "delivery_method")
  private String deliveryMethod;

  @Column(name = "delivered_at")
  private Instant deliveredAt;
}
