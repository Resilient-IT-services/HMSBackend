package com.res.hms.finance.infrastructure.repository;

import com.res.hms.core.repository.BaseRepository;
import com.res.hms.finance.domain.Invoice;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends BaseRepository<Invoice> {
  Optional<Invoice> findByHospitalIdAndInvoiceNumber(UUID hospitalId, String invoiceNumber);

  @org.springframework.data.jpa.repository.Query(
      "SELECT i FROM Invoice i WHERE i.visitId = :visitId")
  java.util.Optional<Invoice> findByVisitId(
      @org.springframework.data.repository.query.Param("visitId") UUID visitId);

  @org.springframework.data.jpa.repository.Query("SELECT SUM(i.totalAmount) FROM Invoice i")
  java.math.BigDecimal sumTotalAmount();
}
