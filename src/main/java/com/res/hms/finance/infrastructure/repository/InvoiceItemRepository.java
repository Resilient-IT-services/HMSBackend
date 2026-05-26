package com.res.hms.finance.infrastructure.repository;

import com.res.hms.core.repository.BaseRepository;
import com.res.hms.finance.domain.FinanceSourceType;
import com.res.hms.finance.domain.InvoiceItem;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceItemRepository extends BaseRepository<InvoiceItem> {
  List<InvoiceItem> findAllByInvoiceId(UUID invoiceId);

  @org.springframework.data.jpa.repository.Query(
      "SELECT new com.res.hms.finance.application.dto.RevenueReportDTO(i.doctorName, COUNT(i), SUM(i.netTotal)) "
          + "FROM InvoiceItem i "
          + "WHERE i.hospitalId = :hospitalId AND i.category = 'CONSULTATION' "
          + "AND (:doctorId IS NULL OR i.doctorId = :doctorId) "
          + "AND i.createdAt >= :startDate AND i.createdAt <= :endDate "
          + "GROUP BY i.doctorName, i.doctorId")
  List<com.res.hms.finance.application.dto.RevenueReportDTO> getDoctorRevenueReport(
      @org.springframework.data.repository.query.Param("hospitalId") UUID hospitalId,
      @org.springframework.data.repository.query.Param("doctorId") UUID doctorId,
      @org.springframework.data.repository.query.Param("startDate") java.time.Instant startDate,
      @org.springframework.data.repository.query.Param("endDate") java.time.Instant endDate);

  @org.springframework.data.jpa.repository.Query(
      "SELECT new com.res.hms.finance.application.dto.RevenueReportDTO('Pharmacy Sales', COUNT(i), SUM(i.netTotal)) "
          + "FROM InvoiceItem i "
          + "WHERE i.hospitalId = :hospitalId AND i.category = 'PHARMACY' "
          + "AND i.createdAt >= :startDate AND i.createdAt <= :endDate")
  List<com.res.hms.finance.application.dto.RevenueReportDTO> getPharmacyRevenueReport(
      @org.springframework.data.repository.query.Param("hospitalId") UUID hospitalId,
      @org.springframework.data.repository.query.Param("startDate") java.time.Instant startDate,
      @org.springframework.data.repository.query.Param("endDate") java.time.Instant endDate);

  @org.springframework.data.jpa.repository.Query(
      "SELECT COUNT(i), SUM(i.netTotal) FROM InvoiceItem i "
          + "WHERE i.doctorId = :doctorId AND i.category = 'CONSULTATION' "
          + "AND i.createdAt >= :startDate AND i.createdAt <= :endDate")
  List<Object[]> getDoctorRevenueSummary(
      @org.springframework.data.repository.query.Param("doctorId") UUID doctorId,
      @org.springframework.data.repository.query.Param("startDate") java.time.Instant startDate,
      @org.springframework.data.repository.query.Param("endDate") java.time.Instant endDate);

  boolean existsBySourceTypeAndSourceId(FinanceSourceType sourceType, UUID sourceId);
}
