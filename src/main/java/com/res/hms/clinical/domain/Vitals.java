package com.res.hms.clinical.domain;

import com.res.hms.core.domain.HospitalScopedEntity;
import com.res.hms.core.domain.TemperatureUnit;
import com.res.hms.core.domain.WeightUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "vitals")
public class Vitals extends HospitalScopedEntity {

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "id")
  private Appointment appointment;

  @Column(name = "blood_pressure")
  private String bloodPressure;

  @Column(name = "systolic")
  private Integer systolic;

  @Column(name = "diastolic")
  private Integer diastolic;

  @Column(name = "heart_rate")
  private Integer heartRate;

  @Column(name = "oxygen_level")
  private java.math.BigDecimal oxygenLevel;

  @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
  @Column(name = "temperature_unit")
  private TemperatureUnit temperatureUnit = TemperatureUnit.CELSIUS;

  private java.math.BigDecimal temperature;

  @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
  @Column(name = "weight_unit")
  private WeightUnit weightUnit = WeightUnit.KG;

  private java.math.BigDecimal weight;
}
