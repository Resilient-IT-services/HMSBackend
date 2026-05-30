package com.res.hms.clinical.domain;

import com.res.hms.core.domain.HospitalScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "appointment_charge_item", schema = "appointment")
public class AppointmentChargeItem extends HospitalScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "charge_id", nullable = false)
  private AppointmentCharge charge;

  @Column(name = "item_name", nullable = false)
  private String itemName;

  @Column(name = "amount", nullable = false, precision = 10, scale = 2)
  private BigDecimal amount = BigDecimal.ZERO;

  @Column(name = "quantity", nullable = false)
  private Integer quantity = 1;

  @Column(name = "subtotal", nullable = false, precision = 13, scale = 2)
  private BigDecimal subtotal = BigDecimal.ZERO;
}
