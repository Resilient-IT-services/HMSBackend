package com.res.hms.clinical.domain;

import com.res.hms.tax.domain.BaseTaxSnapshot;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "appointment_charge_tax", schema = "appointment")
public class AppointmentChargeTax extends BaseTaxSnapshot {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "charge_id", nullable = false)
  private AppointmentCharge charge;
}
