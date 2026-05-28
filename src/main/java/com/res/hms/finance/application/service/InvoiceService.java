package com.res.hms.finance.application.service;

import com.res.hms.finance.application.dto.InvoiceDTO;
import com.res.hms.finance.application.dto.InvoiceItemDTO;
import com.res.hms.finance.domain.Invoice;
import com.res.hms.finance.domain.InvoiceItem;
import com.res.hms.finance.domain.InvoiceItemCategory;
import com.res.hms.finance.domain.InvoiceStatus;
import com.res.hms.finance.infrastructure.repository.InvoiceItemRepository;
import com.res.hms.finance.infrastructure.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

  private final InvoiceRepository invoiceRepository;
  private final InvoiceItemRepository invoiceItemRepository;
  private final InvoicePdfExportService invoicePdfExportService;
  private final com.res.hms.finance.infrastructure.repository.HsnMasterRepository
      hsnMasterRepository;

  @Transactional(readOnly = true)
  public List<InvoiceDTO> getInvoicesByPatient(UUID patientId) {
    return invoiceRepository.findAll().stream()
        .filter(i -> patientId.equals(i.getPatientId()))
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public InvoiceDTO getById(UUID id) {
    return invoiceRepository
        .findById(id)
        .map(this::toDto)
        .orElseThrow(() -> new RuntimeException("Invoice not found"));
  }

  @Transactional(readOnly = true)
  public InvoiceDTO getByVisit(UUID visitId) {
    return invoiceRepository
        .findByVisitId(visitId)
        .map(this::toDto)
        .orElseThrow(
            () ->
                new com.res.hms.core.exception.ApiException(
                    "NOT_FOUND", "Invoice not found for visit", HttpStatus.NOT_FOUND));
  }

  public InvoiceDTO toDto(Invoice invoice) {
    return InvoiceDTO.builder()
        .id(invoice.getId())
        .patientId(invoice.getPatientId())
        .patientName(invoice.getPatient() != null ? invoice.getPatient().getFullName() : null)
        .visitId(invoice.getVisitId())
        .invoiceNumber(invoice.getInvoiceNumber())
        .status(invoice.getStatus())
        .subtotalAmount(invoice.getSubtotalAmount())
        .taxAmount(invoice.getTaxAmount())
        .discountAmount(invoice.getDiscountAmount())
        .totalAmount(invoice.getTotalAmount())
        .amountPaid(invoice.getAmountPaid())
        .amountDue(invoice.getAmountDue())
        .dueDate(invoice.getDueDate())
        .createdAt(invoice.getCreatedAt())
        .items(invoice.getItems().stream().map(this::toItemDto).collect(Collectors.toList()))
        .build();
  }

  private InvoiceItemDTO toItemDto(InvoiceItem item) {
    return InvoiceItemDTO.builder()
        .id(item.getId())
        .itemDescription(item.getItemDescription())
        .hsnCode(item.getHsnCode())
        .category(item.getCategory())
        .quantity(item.getQuantity())
        .unitPrice(item.getUnitPrice())
        .subtotal(item.getSubtotal())
        .taxPercentage(item.getTaxPercentage())
        .taxAmount(item.getTaxAmount())
        .discountAmount(item.getDiscountAmount())
        .netTotal(item.getNetTotal())
        .sourceType(item.getSourceType())
        .sourceId(item.getSourceId())
        .build();
  }

  @Transactional
  public Invoice getOrCreateInvoice(UUID hospitalId, UUID patientId, UUID visitId) {
    if (visitId != null) {
      return invoiceRepository
          .findByVisitId(visitId)
          .filter(i -> i.getStatus() != InvoiceStatus.CANCELLED)
          .orElseGet(() -> createInvoice(hospitalId, patientId, visitId));
    }
    return createInvoice(hospitalId, patientId, null);
  }

  @Transactional
  public Invoice createInvoice(UUID hospitalId, UUID patientId, UUID visitId) {
    Invoice invoice = new Invoice();
    invoice.setHospitalId(hospitalId);
    invoice.setPatientId(patientId);
    invoice.setVisitId(visitId);
    invoice.setInvoiceNumber("INV-" + System.currentTimeMillis());
    invoice.setStatus(InvoiceStatus.UNPAID);
    invoice.setSubtotalAmount(BigDecimal.ZERO);
    invoice.setTaxAmount(BigDecimal.ZERO);
    invoice.setDiscountAmount(BigDecimal.ZERO);
    invoice.setTotalAmount(BigDecimal.ZERO);
    invoice.setAmountPaid(BigDecimal.ZERO);
    Invoice saved = invoiceRepository.saveAndFlush(invoice);
    return saved;
  }

  @Transactional
  public void addItem(
      UUID invoiceId,
      String description,
      InvoiceItemCategory category,
      BigDecimal quantity,
      BigDecimal unitPrice,
      String hsnCode,
      BigDecimal taxPercentage,
      BigDecimal taxAmount,
      BigDecimal discountAmount,
      com.res.hms.finance.domain.FinanceSourceType sourceType,
      UUID sourceId,
      UUID doctorId,
      String doctorName) {
    Invoice invoice =
        invoiceRepository
            .findById(invoiceId)
            .orElseThrow(() -> new RuntimeException("Invoice not found"));

    InvoiceItem item = new InvoiceItem();
    item.setHospitalId(invoice.getHospitalId());
    item.setInvoiceId(invoiceId);
    item.setItemDescription(description);
    item.setHsnCode(hsnCode);
    item.setCategory(category);
    item.setQuantity(quantity);
    item.setUnitPrice(unitPrice);

    BigDecimal subtotal = unitPrice.multiply(quantity);
    item.setSubtotal(subtotal);

    BigDecimal finalTaxPercentage = taxPercentage;
    if (finalTaxPercentage == null && hsnCode != null) {
      finalTaxPercentage =
          hsnMasterRepository
              .findByHospitalIdAndHsnCodeAndActiveTrue(invoice.getHospitalId(), hsnCode)
              .map(com.res.hms.finance.domain.HsnMaster::getTaxPercentage)
              .orElse(BigDecimal.ZERO);
    }
    if (finalTaxPercentage == null) {
      finalTaxPercentage = BigDecimal.ZERO;
    }
    item.setTaxPercentage(finalTaxPercentage);

    BigDecimal finalTaxAmount;
    if (taxAmount != null) {
      finalTaxAmount = taxAmount;
    } else if (finalTaxPercentage.compareTo(BigDecimal.ZERO) > 0) {
      finalTaxAmount =
          subtotal
              .multiply(finalTaxPercentage)
              .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
    } else {
      finalTaxAmount = BigDecimal.ZERO;
    }
    item.setTaxAmount(finalTaxAmount);

    item.setDiscountAmount(discountAmount != null ? discountAmount : BigDecimal.ZERO);
    item.setNetTotal(
        item.getSubtotal().add(item.getTaxAmount()).subtract(item.getDiscountAmount()));
    item.setSourceType(sourceType);
    item.setSourceId(sourceId);
    item.setDoctorId(doctorId);
    item.setDoctorName(doctorName);
    item.setInvoice(invoice);

    invoiceItemRepository.save(item);
    invoice.getItems().add(item);

    updateTotals(invoice);
  }

  @Transactional
  public void updateTotals(Invoice invoice) {
    BigDecimal subtotal =
        invoice.getItems().stream()
            .map(InvoiceItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal tax =
        invoice.getItems().stream()
            .map(InvoiceItem::getTaxAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal discount =
        invoice.getItems().stream()
            .map(InvoiceItem::getDiscountAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    invoice.setSubtotalAmount(subtotal);
    invoice.setTaxAmount(tax);
    invoice.setDiscountAmount(discount);
    invoice.setTotalAmount(subtotal.add(tax).subtract(discount));

    invoiceRepository.save(invoice);
  }

  @Transactional
  public void cancelInvoice(UUID invoiceId) {
    invoiceRepository
        .findById(invoiceId)
        .ifPresent(
            invoice -> {
              invoice.setStatus(InvoiceStatus.CANCELLED);
              invoiceRepository.save(invoice);
            });
  }

  @Transactional(readOnly = true)
  public byte[] exportInvoicePdf(UUID invoiceId) {
    Invoice invoice =
        invoiceRepository
            .findById(invoiceId)
            .orElseThrow(() -> new RuntimeException("Invoice not found"));
    return invoicePdfExportService.exportInvoicePdf(invoice);
  }
}
