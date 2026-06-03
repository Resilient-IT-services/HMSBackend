package com.res.hms.lab.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

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
