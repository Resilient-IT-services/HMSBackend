package com.res.hms.clinical.domain;

import com.res.hms.core.domain.HospitalScopedEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "appointment_charge", schema = "appointment")
public class AppointmentCharge extends HospitalScopedEntity {

  @Column(name = "patient_id", nullable = false)
  private UUID patientId;

  @Column(name = "appointment_id")
  private UUID appointmentId;

  @Column(name = "invoice_id")
  private UUID invoiceId;

  @Column(name = "doctor_id")
  private UUID doctorId;

  @Column(name = "doctor_name")
  private String doctorName;

  @Column(name = "source_type", nullable = false)
  private String sourceType;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalAmount = BigDecimal.ZERO;

  @Column(name = "discount", precision = 12, scale = 2)
  private BigDecimal discount = BigDecimal.ZERO;

  @Column(name = "payment_method")
  private String paymentMethod;

  @OneToMany(mappedBy = "charge", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<AppointmentChargeItem> items = new ArrayList<>();
}
