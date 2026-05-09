package com.res.hms.finance.infrastructure.repository;

import com.res.hms.core.repository.BaseRepository;
import com.res.hms.finance.domain.Billing;
import com.res.hms.finance.application.dto.PaymentSummaryDTO;
import com.res.hms.finance.application.dto.RevenueReportDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingRepository extends BaseRepository<Billing> {
  Optional<Billing> findByAppointmentIdAndHospitalId(UUID appointmentId, UUID hospitalId);

  @Query(
      "SELECT new com.res.hms.finance.application.dto.RevenueReportDTO(d.user.fullName, COUNT(b), SUM(b.totalAmount - COALESCE(b.discount, 0))) "
          + "FROM Billing b JOIN b.appointment a JOIN a.doctorProfile d "
          + "WHERE b.hospitalId = :hospitalId AND b.status = 'PAID' "
          + "AND (:doctorId IS NULL OR d.id = :doctorId) "
          + "AND b.createdAt >= :startDate AND b.createdAt <= :endDate "
          + "GROUP BY d.user.fullName")
  List<RevenueReportDTO> getDoctorRevenueReport(
      @Param("hospitalId") UUID hospitalId,
      @Param("doctorId") UUID doctorId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);

  @Query(
      "SELECT new com.res.hms.finance.application.dto.PaymentSummaryDTO(b.paymentMethod, COUNT(b), SUM(b.totalAmount - COALESCE(b.discount, 0))) "
          + "FROM Billing b "
          + "WHERE b.hospitalId = :hospitalId AND b.status = 'PAID' "
          + "AND b.createdAt >= :startDate AND b.createdAt <= :endDate "
          + "GROUP BY b.paymentMethod")
  List<PaymentSummaryDTO> getPaymentSummaryReport(
      @Param("hospitalId") UUID hospitalId,
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);
}
