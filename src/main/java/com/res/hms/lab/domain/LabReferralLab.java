package com.res.hms.lab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
