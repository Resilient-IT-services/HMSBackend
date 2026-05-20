package com.res.hms.pharmacy.application.service;

import com.res.hms.clinical.application.prescription.repository.PrescriptionRepository;
import com.res.hms.clinical.infrastructure.repository.AppointmentRepository;
import com.res.hms.core.exception.ApiException;
import com.res.hms.core.tenant.HospitalContext;
import com.res.hms.finance.application.dto.RecordPaymentRequest;
import com.res.hms.finance.application.service.InvoiceService;
import com.res.hms.finance.application.service.PaymentService;
import com.res.hms.finance.domain.FinanceSourceType;
import com.res.hms.finance.domain.Invoice;
import com.res.hms.finance.domain.InvoiceItemCategory;
import com.res.hms.pharmacy.application.dto.QuickPharmacySaleRequest;
import com.res.hms.pharmacy.application.dto.QuickPharmacySaleResponse;
import com.res.hms.pharmacy.domain.PharmacySale;
import com.res.hms.pharmacy.domain.PharmacySaleItem;
import com.res.hms.pharmacy.domain.PharmacySaleStatus;
import com.res.hms.pharmacy.domain.PharmacyStock;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacySaleRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyStockRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuickPharmacySaleService {

  private final PharmacySaleRepository pharmacySaleRepository;
  private final PharmacyStockRepository pharmacyStockRepository;
  private final MedicineRepository medicineRepository;
  private final PrescriptionRepository prescriptionRepository;
  private final AppointmentRepository appointmentRepository;
  private final InvoiceService invoiceService;
  private final PaymentService paymentService;
  private final HospitalContext hospitalContext;

  @Transactional
  public QuickPharmacySaleResponse createQuickSale(QuickPharmacySaleRequest request) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    UUID userId = hospitalContext.currentUserIdOrNull();

    requireLoggedInUser(userId);
    validatePrescription(hospitalId, request.getPrescriptionId());

    PharmacySale sale = initializeSale(hospitalId, request);
    sale.setVisitId(resolveVisitId(request, sale));
    sale.setTotalAmount(processItems(hospitalId, request.getItems(), sale));

    PharmacySale savedSale = pharmacySaleRepository.save(sale);
    Invoice invoice = createInvoice(request, hospitalId, savedSale);
    addInvoiceItems(savedSale, invoice);
    markPrescriptionDispensed(request.getPrescriptionId(), hospitalId);

    PaymentReceipt paymentReceipt = recordPayment(request, hospitalId, userId, invoice);

    var response = toQuickSaleResponse(savedSale);
    response.setInvoiceNumber(invoice.getInvoiceNumber());
    response.setPaymentId(paymentReceipt.paymentId());
    response.setReceiptNumber(paymentReceipt.receiptNumber());
    return response;
  }

  private void requireLoggedInUser(UUID userId) {
    if (userId == null) {
      throw new ApiException(
          "AUTH_ERROR", "User must be logged in to record sales", HttpStatus.UNAUTHORIZED);
    }
  }

  private void validatePrescription(UUID hospitalId, UUID prescriptionId) {
    if (prescriptionId == null) {
      return;
    }
    var prescription =
        prescriptionRepository
            .findByIdAndHospitalId(prescriptionId, hospitalId)
            .orElseThrow(
                () ->
                    new ApiException("NOT_FOUND", "Prescription not found", HttpStatus.NOT_FOUND));

    if ("DISPENSED".equals(prescription.getStatus())) {
      throw new ApiException(
          "ALREADY_DISPENSED",
          "This prescription has already been dispensed.",
          HttpStatus.CONFLICT);
    }
  }

  private PharmacySale initializeSale(UUID hospitalId, QuickPharmacySaleRequest request) {
    PharmacySale sale = new PharmacySale();
    sale.setHospitalId(hospitalId);
    sale.setSaleType(request.getSaleType());
    sale.setPatientId(request.getPatientId());
    sale.setPrescriptionId(request.getPrescriptionId());
    sale.setDoctorId(request.getDoctorId());
    sale.setExternalDoctorName(request.getExternalDoctorName());
    sale.setCustomerName(request.getCustomerName());
    sale.setCustomerPhone(request.getCustomerPhone());
    sale.setNotes(request.getNotes());
    sale.setStatus(PharmacySaleStatus.COMPLETED);
    sale.setSaleNumber("SALE-" + System.currentTimeMillis());
    return sale;
  }

  private UUID resolveVisitId(QuickPharmacySaleRequest request, PharmacySale sale) {
    if (request.getPrescriptionId() == null) {
      return request.getVisitId();
    }
    appointmentRepository
        .findById(request.getPrescriptionId())
        .ifPresent(
            app -> {
              if (app.getVisitId() != null) {
                sale.setVisitId(app.getVisitId());
              }
            });
    return sale.getVisitId();
  }

  private BigDecimal processItems(
      UUID hospitalId, List<QuickPharmacySaleRequest.Item> itemDtos, PharmacySale sale) {
    BigDecimal totalAmount = BigDecimal.ZERO;

    for (QuickPharmacySaleRequest.Item itemDto : itemDtos) {
      PharmacyStock stock =
          pharmacyStockRepository
              .findByIdAndHospitalId(itemDto.getMedicineBatchId(), hospitalId)
              .orElseThrow(
                  () ->
                      new ApiException("NOT_FOUND", "Stock batch not found", HttpStatus.NOT_FOUND));

      if (stock.getQuantity() < itemDto.getQuantity()) {
        throw new ApiException(
            "INSUFFICIENT_STOCK",
            "Insufficient stock for " + stock.getMedicine().getName(),
            HttpStatus.CONFLICT);
      }

      // Update stock
      stock.setQuantity(stock.getQuantity() - itemDto.getQuantity());
      pharmacyStockRepository.save(stock);

      // Create Sale Item
      PharmacySaleItem saleItem = new PharmacySaleItem();
      saleItem.setSale(sale);
      saleItem.setMedicine(stock.getMedicine());
      saleItem.setStock(stock);
      saleItem.setQuantity(itemDto.getQuantity());
      saleItem.setUnitPrice(itemDto.getUnitPrice());
      saleItem.setDiscountAmount(
          itemDto.getDiscountAmount() != null ? itemDto.getDiscountAmount() : BigDecimal.ZERO);
      saleItem.setTaxAmount(
          itemDto.getTaxAmount() != null ? itemDto.getTaxAmount() : BigDecimal.ZERO);

      BigDecimal itemSubtotal =
          itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
      saleItem.setSubtotal(itemSubtotal);

      BigDecimal itemNet =
          itemSubtotal.add(saleItem.getTaxAmount()).subtract(saleItem.getDiscountAmount());
      saleItem.setNetAmount(itemNet);

      sale.getItems().add(saleItem);
      totalAmount = totalAmount.add(itemNet);
    }

    return totalAmount;
  }

  private Invoice createInvoice(
      QuickPharmacySaleRequest request, UUID hospitalId, PharmacySale savedSale) {
    Invoice invoice =
        invoiceService.getOrCreateInvoice(
            hospitalId, request.getPatientId(), savedSale.getVisitId());
    savedSale.setInvoiceId(invoice.getId());
    savedSale.setSaleNumber(invoice.getInvoiceNumber());
    return invoice;
  }

  private void addInvoiceItems(PharmacySale savedSale, Invoice invoice) {
    for (PharmacySaleItem saleItem : savedSale.getItems()) {
      invoiceService.addItem(
          invoice.getId(),
          saleItem.getMedicine().getName() + " (" + saleItem.getStock().getBatchNumber() + ")",
          InvoiceItemCategory.PHARMACY,
          BigDecimal.valueOf(saleItem.getQuantity()),
          saleItem.getUnitPrice(),
          FinanceSourceType.PHARMACY_SALE_ITEM,
          saleItem.getId(),
          savedSale.getDoctorId(),
          savedSale.getExternalDoctorName());
    }
  }

  private void markPrescriptionDispensed(UUID prescriptionId, UUID hospitalId) {
    if (prescriptionId == null) {
      return;
    }
    prescriptionRepository
        .findByIdAndHospitalId(prescriptionId, hospitalId)
        .ifPresent(
            p -> {
              p.setStatus(com.res.hms.clinical.domain.PrescriptionStatus.DISPENSED);
              prescriptionRepository.save(p);
            });
  }

  private PaymentReceipt recordPayment(
      QuickPharmacySaleRequest request, UUID hospitalId, UUID userId, Invoice invoice) {
    if (request.getPayment() != null
        && request.getPayment().getAmount().compareTo(BigDecimal.ZERO) > 0) {
      RecordPaymentRequest paymentReq = new RecordPaymentRequest();
      paymentReq.setInvoiceId(invoice.getId());
      paymentReq.setPatientId(request.getPatientId());
      paymentReq.setVisitId(request.getVisitId());
      paymentReq.setAmount(request.getPayment().getAmount());
      paymentReq.setPaymentMode(request.getPayment().getMode());
      paymentReq.setReferenceNumber(request.getPayment().getReferenceNumber());

      var paymentDto = paymentService.recordPayment(hospitalId, paymentReq, userId);
      return new PaymentReceipt(paymentDto.getId(), paymentDto.getReceiptNumber());
    }
    return new PaymentReceipt(null, null);
  }

  private record PaymentReceipt(UUID paymentId, String receiptNumber) {}

  @Transactional(readOnly = true)
  public QuickPharmacySaleResponse getSale(UUID id) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    PharmacySale sale =
        pharmacySaleRepository
            .findByIdAndHospitalId(id, hospitalId)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Sale not found", HttpStatus.NOT_FOUND));

    return toQuickSaleResponse(sale);
  }

  @Transactional(readOnly = true)
  public QuickPharmacySaleResponse getSaleByPrescription(UUID prescriptionId) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    PharmacySale sale =
        pharmacySaleRepository
            .findByPrescriptionIdAndHospitalId(prescriptionId, hospitalId)
            .orElseThrow(
                () ->
                    new ApiException(
                        "NOT_FOUND",
                        "No dispensing record found for this prescription",
                        HttpStatus.NOT_FOUND));

    return toQuickSaleResponse(sale);
  }

  private QuickPharmacySaleResponse toQuickSaleResponse(PharmacySale sale) {
    List<QuickPharmacySaleResponse.Item> items =
        sale.getItems().stream()
            .map(
                item ->
                    QuickPharmacySaleResponse.Item.builder()
                        .id(item.getId())
                        .medicineName(item.getMedicine().getName())
                        .batchNumber(item.getStock().getBatchNumber())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .discountAmount(item.getDiscountAmount())
                        .netAmount(item.getNetAmount())
                        .build())
            .toList();

    return QuickPharmacySaleResponse.builder()
        .saleId(sale.getId())
        .saleNumber(sale.getSaleNumber())
        .invoiceId(sale.getInvoiceId())
        .totalAmount(sale.getTotalAmount())
        .status(sale.getStatus().name())
        .customerName(sale.getCustomerName())
        .customerPhone(sale.getCustomerPhone())
        .externalDoctorName(sale.getExternalDoctorName())
        .createdAt(sale.getCreatedAt())
        .items(items)
        .build();
  }

  @Transactional(readOnly = true)
  public List<Object[]> searchRecentCustomers(String search) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return pharmacySaleRepository.searchRecentWalkinCustomers(hospitalId, search);
  }
}
