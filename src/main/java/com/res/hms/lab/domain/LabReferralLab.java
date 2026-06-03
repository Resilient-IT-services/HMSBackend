package com.res.hms.lab.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "lab_referral_lab", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabReferralLab {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "lab_name")
  private String labName;

  @Column(name = "contact_person")
  private String contactPerson;

  @Column(name = "email")
  private String email;
}
