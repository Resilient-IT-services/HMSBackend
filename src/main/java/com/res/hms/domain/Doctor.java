package com.res.hms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "doctor")
public class Doctor extends BaseEntity {

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id", unique = true)
  private UserAccount user;

  @Column(nullable = false)
  private String specialization;

  @Column(nullable = false)
  private BigDecimal consultationFee = BigDecimal.ZERO;

  @Column(nullable = false)
  private Integer followUpDays = 0;

  public String getFullName() {

    return user != null ? user.getFullName() : null;
  }
}
