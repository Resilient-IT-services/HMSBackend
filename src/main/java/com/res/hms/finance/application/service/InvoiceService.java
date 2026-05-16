package com.res.hms.finance.application.service;

import com.res.hms.finance.domain.Invoice;
import com.res.hms.finance.domain.InvoiceStatus;
import com.res.hms.finance.infrastructure.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    @Transactional
    public Invoice createInvoice(UUID hospitalId, UUID patientId, UUID visitId) {
        Invoice invoice = new Invoice();
        invoice.setHospitalId(hospitalId);
        invoice.setPatientId(patientId);
        invoice.setVisitId(visitId);
        invoice.setInvoiceNumber("INV-" + System.currentTimeMillis());
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setTotalAmount(BigDecimal.ZERO);
        invoice.setAmountPaid(BigDecimal.ZERO);
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public void cancelInvoice(UUID invoiceId) {
        invoiceRepository.findById(invoiceId).ifPresent(invoice -> {
            invoice.setStatus(InvoiceStatus.CANCELLED);
            invoiceRepository.save(invoice);
        });
    }
}
