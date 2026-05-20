package com.res.hms.finance.infrastructure.repository;

import com.res.hms.core.repository.BaseRepository;
import com.res.hms.finance.domain.Payment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends BaseRepository<Payment> {
    Optional<Payment> findByHospitalIdAndReceiptNumber(UUID hospitalId, String receiptNumber);

    @org.springframework.data.jpa.repository.Query(
        "SELECT new com.res.hms.finance.application.dto.PaymentSummaryDTO(CAST(p.paymentMode AS string), COUNT(p), SUM(p.amount)) "
            + "FROM Payment p "
            + "WHERE p.hospitalId = :hospitalId AND p.status = 'SUCCESS' "
            + "AND p.paymentDate >= :startDate AND p.paymentDate <= :endDate "
            + "GROUP BY p.paymentMode")
    List<com.res.hms.finance.application.dto.PaymentSummaryDTO> getPaymentSummaryReport(
        @org.springframework.data.repository.query.Param("hospitalId") UUID hospitalId,
        @org.springframework.data.repository.query.Param("startDate") java.time.Instant startDate,
        @org.springframework.data.repository.query.Param("endDate") java.time.Instant endDate);

    @org.springframework.data.jpa.repository.Query(
        "SELECT new com.res.hms.finance.application.dto.PaymentSummaryDTO(p.collector.fullName, COUNT(p), SUM(p.amount)) "
            + "FROM Payment p "
            + "WHERE p.hospitalId = :hospitalId AND p.status = 'SUCCESS' "
            + "AND p.paymentDate >= :startDate AND p.paymentDate <= :endDate "
            + "GROUP BY p.collector.fullName")
    List<com.res.hms.finance.application.dto.PaymentSummaryDTO> getPaymentCollectorReport(
        @org.springframework.data.repository.query.Param("hospitalId") UUID hospitalId,
        @org.springframework.data.repository.query.Param("startDate") java.time.Instant startDate,
        @org.springframework.data.repository.query.Param("endDate") java.time.Instant endDate);
}
