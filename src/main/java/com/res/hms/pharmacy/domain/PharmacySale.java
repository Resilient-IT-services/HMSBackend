package com.res.hms.pharmacy.domain;

import com.res.hms.core.domain.HospitalScopedEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@Table(name = "pharmacy_sale")
public class PharmacySale extends HospitalScopedEntity {

  @Column(name = "sale_number")
  private String saleNumber;

  @Column(name = "sale_type")
  private String saleType; // VISIT, WALKIN

  @Column(name = "patient_id")
  private UUID patientId;

  @Column(name = "visit_id")
  private UUID visitId;

  @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
  @jakarta.persistence.JoinColumn(name = "visit_id", insertable = false, updatable = false)
  private com.res.hms.clinical.domain.PatientVisit visit;

  @Column(name = "prescription_id")
  private UUID prescriptionId;

  @Column(name = "doctor_id")
  private UUID doctorId;

  @Column(name = "external_doctor_name")
  private String externalDoctorName;

  @Column(name = "customer_name")
  private String customerName;

  @Column(name = "customer_phone")
  private String customerPhone;

  @Column(name = "invoice_id")
  private UUID invoiceId;

  @Column(name = "status")
  private String status;

  @Column(name = "notes")
  private String notes;

  @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalAmount = BigDecimal.ZERO;

  @OneToMany(
      mappedBy = "sale",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private List<PharmacySaleItem> items = new ArrayList<>();
}
