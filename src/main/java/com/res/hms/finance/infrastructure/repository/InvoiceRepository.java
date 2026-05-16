package com.res.hms.finance.infrastructure.repository;

import com.res.hms.core.repository.BaseRepository;
import com.res.hms.finance.domain.Invoice;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends BaseRepository<Invoice> {
    Optional<Invoice> findByHospitalIdAndInvoiceNumber(UUID hospitalId, String invoiceNumber);
}
