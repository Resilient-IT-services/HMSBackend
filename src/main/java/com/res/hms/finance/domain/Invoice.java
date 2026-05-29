package com.res.hms.finance.domain;

import com.res.hms.clinical.domain.Patient;
import com.res.hms.clinical.domain.PatientVisit;
import com.res.hms.core.domain.HospitalScopedEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "invoice")
public class Invoice extends HospitalScopedEntity {

  @Column(name = "patient_id")
  private UUID patientId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "patient_id", insertable = false, updatable = false)
  private Patient patient;

  @Column(name = "customer_name")
  private String customerName;

  @Column(name = "visit_id")
  private UUID visitId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "visit_id", insertable = false, updatable = false)
  private PatientVisit visit;

  @Column(name = "invoice_number", nullable = false)
  private String invoiceNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private InvoiceStatus status;

  @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal subtotalAmount = BigDecimal.ZERO;

  @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal taxAmount = BigDecimal.ZERO;

  @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal discountAmount = BigDecimal.ZERO;

  @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalAmount = BigDecimal.ZERO;

  @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
  private BigDecimal amountPaid = BigDecimal.ZERO;

  @Column(name = "amount_due", insertable = false, updatable = false)
  private BigDecimal amountDue;

  @Column(name = "due_date")
  private LocalDateTime dueDate;

  @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<InvoiceItem> items = new ArrayList<>();
}
