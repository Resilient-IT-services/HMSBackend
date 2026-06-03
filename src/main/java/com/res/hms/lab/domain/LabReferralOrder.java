package com.res.hms.lab.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "lab_referral_order", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabReferralOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "order_test_id")
  private UUID orderTestId;

  @Column(name = "referral_lab_id")
  private UUID referralLabId;

  @Column(name = "referred_at")
  private Instant referredAt;

  @Column(name = "status")
  private String status;
}
