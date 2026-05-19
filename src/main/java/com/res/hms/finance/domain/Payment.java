package com.res.hms.finance.domain;

import com.res.hms.clinical.domain.Patient;
import com.res.hms.core.domain.HospitalScopedEntity;
import com.res.hms.iam.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "payment")
public class Payment extends HospitalScopedEntity {

    @Column(name = "patient_id")
    private UUID patientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", insertable = false, updatable = false)
    private Patient patient;

    @Column(name = "visit_id")
    private UUID visitId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id", insertable = false, updatable = false)
    private com.res.hms.clinical.domain.PatientVisit visit;

    @Column(name = "receipt_number", nullable = false)
    private String receiptNumber;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false)
    private PaymentMode paymentMode;

    @Column(name = "reference_number")
    private String referenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "payment_date", nullable = false)
    private java.time.Instant paymentDate;

    @Column(name = "collected_by", nullable = false)
    private UUID collectedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collected_by", insertable = false, updatable = false)
    private UserAccount collector;
}
