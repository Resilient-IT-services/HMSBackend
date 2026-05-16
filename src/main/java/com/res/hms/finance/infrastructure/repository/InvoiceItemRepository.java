package com.res.hms.finance.infrastructure.repository;

import com.res.hms.core.repository.BaseRepository;
import com.res.hms.finance.domain.InvoiceItem;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceItemRepository extends BaseRepository<InvoiceItem> {
    List<InvoiceItem> findAllByInvoiceId(UUID invoiceId);
}
