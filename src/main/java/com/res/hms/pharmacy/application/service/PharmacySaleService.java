package com.res.hms.pharmacy.application.service;

import com.res.hms.clinical.application.prescription.repository.PrescriptionRepository;
import com.res.hms.clinical.domain.PrescriptionStatus;
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
import com.res.hms.pharmacy.domain.PharmacySaleItemTax;
import com.res.hms.pharmacy.domain.PharmacySaleStatus;
import com.res.hms.pharmacy.domain.PharmacyStock;
import com.res.hms.pharmacy.infrastructure.repository.MedicineRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyReturnRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacySaleItemRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacySaleRepository;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyStockRepository;
import com.res.hms.tax.application.dto.TaxCalculationRequest;
import com.res.hms.tax.application.dto.TaxCalculationResult;
import com.res.hms.tax.application.service.TaxEngineService;
import java.math.BigDecimal;
import java.time.Instant;
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
public class PharmacySaleService {

  private final PharmacySaleRepository pharmacySaleRepository;
  private final PharmacyStockRepository pharmacyStockRepository;
  private final MedicineRepository medicineRepository;
  private final PharmacySaleItemRepository pharmacySaleItemRepository;
  private final PharmacyReturnRepository pharmacyReturnRepository;
  private final PrescriptionRepository prescriptionRepository;
  private final AppointmentRepository appointmentRepository;
  private final com.res.hms.hospital.infrastructure.repository.HospitalRepository
      hospitalRepository;
  private final com.res.hms.iam.infrastructure.repository.UserAccountRepository
      userAccountRepository;
  private final InvoiceService invoiceService;
  private final PaymentService paymentService;
  private final HospitalContext hospitalContext;
  private final com.res.hms.clinical.application.service.PatientVisitService patientVisitService;
  private final TaxEngineService taxEngineService;
  private final com.res.hms.clinical.infrastructure.repository.PatientRepository patientRepository;
  private final PharmacyStockService pharmacyStockService;

  @Transactional
  public QuickPharmacySaleResponse createSale(QuickPharmacySaleRequest request) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    UUID userId = hospitalContext.currentUserIdOrNull();

    requireLoggedInUser(userId);
    // Removed strict prescription dispense check to allow multiple bills

    PharmacySale sale = initializeSale(hospitalId, request);
    sale.setVisitId(resolveVisitId(request, sale));
    sale.setTotalAmount(processItems(hospitalId, request.getItems(), sale));

    PharmacySale savedSale = pharmacySaleRepository.save(sale);
    com.res.hms.hospital.domain.Hospital hospital =
        hospitalRepository
            .findById(hospitalId)
            .orElseThrow(
                () -> new ApiException("NOT_FOUND", "Hospital not found", HttpStatus.NOT_FOUND));

    // Calculate and Save Taxes for each item
    for (PharmacySaleItem item : savedSale.getItems()) {
      TaxCalculationRequest taxRequest =
          TaxCalculationRequest.builder()
              .hospitalId(hospitalId)
              .itemType("PHARMACY")
              .itemCode(item.getMedicine().getId().toString())
              .hsnOrSacCode(item.getMedicine().getHsnCode())
              .taxableAmount(item.getTaxableAmount())
              .quantity(BigDecimal.valueOf(item.getQuantity()))
              .unitPrice(item.getUnitPrice())
              .hospitalState(hospital.getState())
              .billingState(hospital.getState()) // Default to intra-state for pharmacy sales
              .invoiceDate(Instant.now())
              .build();

      TaxCalculationResult taxResult = taxEngineService.calculate(taxRequest);

      item.setTaxAmount(taxResult.totalTax());
      item.setNetAmount(item.getTaxableAmount().add(taxResult.totalTax()));

      if (taxResult.components() != null) {
        for (com.res.hms.tax.application.dto.TaxComponent tc : taxResult.components()) {
          PharmacySaleItemTax pit = new PharmacySaleItemTax();
          pit.setPharmacySaleItem(item);
          pit.setTaxCode(tc.taxCode());
          pit.setTaxName(tc.taxName());
          pit.setTaxRate(tc.taxRate());
          pit.setTaxAmount(tc.taxAmount());
          pit.setTaxableAmount(item.getTaxableAmount());
          item.getTaxes().add(pit);
        }
      }
    }

    // Update total sale amount with taxes
    BigDecimal grandTotal =
        savedSale.getItems().stream()
            .map(PharmacySaleItem::getNetAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    savedSale.setTotalAmount(grandTotal);
    pharmacySaleRepository.save(savedSale);

    Invoice invoice = createInvoice(request, hospitalId, savedSale);
    addInvoiceItems(savedSale, invoice);

    PaymentReceipt paymentReceipt =
        recordPayment(request, hospitalId, userId, invoice, savedSale.getTotalAmount());

    // Update prescription status to DISPENSED if it exists
    if (request.getPrescriptionId() != null) {
      prescriptionRepository
          .findById(request.getPrescriptionId())
          .ifPresent(
              pres -> {
                pres.setStatus(PrescriptionStatus.DISPENSED);
                prescriptionRepository.save(pres);
              });
    }

    var response = toQuickSaleResponse(savedSale);
    response.setPaymentId(paymentReceipt.paymentId());
    response.setReceiptNumber(paymentReceipt.receiptNumber());

    // Populate additional details for the response
    if (savedSale.getDoctorId() != null) {
      userAccountRepository
          .findById(savedSale.getDoctorId())
          .ifPresent(u -> response.setDoctorName(u.getFullName()));
    }
    if (savedSale.getPatientId() != null) {
      patientRepository
          .findById(savedSale.getPatientId())
          .ifPresent(p -> response.setPatientCode(p.getPatientCode()));
    }

    return response;
  }

  private void requireLoggedInUser(UUID userId) {
    if (userId == null) {
      throw new ApiException(
          "AUTH_ERROR", "User must be logged in to record sales", HttpStatus.UNAUTHORIZED);
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
    prescriptionRepository
        .findById(request.getPrescriptionId())
        .ifPresent(
            pres -> {
              if (pres.getAppointment() != null && pres.getAppointment().getVisitId() != null) {
                sale.setVisitId(pres.getAppointment().getVisitId());
              }
            });

    if (sale.getVisitId() == null) {
      return request.getVisitId();
    }
    return sale.getVisitId();
  }

  private BigDecimal processItems(
      UUID hospitalId, List<QuickPharmacySaleRequest.Item> itemDtos, PharmacySale sale) {
    BigDecimal totalAmount = BigDecimal.ZERO;
    java.time.LocalDate today = java.time.LocalDate.now();

    for (QuickPharmacySaleRequest.Item itemDto : itemDtos) {
      if (itemDto.getMedicineBatchId() != null) {
        // Specific batch selection
        PharmacyStock stock =
            pharmacyStockRepository
                .findByIdAndHospitalId(itemDto.getMedicineBatchId(), hospitalId)
                .orElseThrow(
                    () ->
                        new ApiException(
                            "NOT_FOUND", "Stock batch not found", HttpStatus.NOT_FOUND));

        totalAmount = totalAmount.add(consumeStock(sale, stock, itemDto.getQuantity(), itemDto));
      } else if (itemDto.getMedicineId() != null) {
        // Auto-batch selection (FIFO)
        List<PharmacyStock> batches =
            pharmacyStockRepository.findDispensableBatchesForUpdate(
                hospitalId, itemDto.getMedicineId(), today);

        int remaining = itemDto.getQuantity();
        for (PharmacyStock batch : batches) {
          int consumed = Math.min(remaining, batch.getQuantity());
          totalAmount = totalAmount.add(consumeStock(sale, batch, consumed, itemDto));
          remaining -= consumed;
          if (remaining == 0) {
            break;
          }
        }

        if (remaining > 0) {
          String medName =
              medicineRepository
                  .findById(itemDto.getMedicineId())
                  .map(com.res.hms.pharmacy.domain.Medicine::getName)
                  .orElse("Medicine");
          throw new ApiException(
              "INSUFFICIENT_STOCK", "Insufficient stock for " + medName, HttpStatus.CONFLICT);
        }
      } else {
        throw new ApiException(
            "BAD_REQUEST",
            "Either medicineId or medicineBatchId is required",
            HttpStatus.BAD_REQUEST);
      }
    }

    return totalAmount;
  }

  private BigDecimal consumeStock(
      PharmacySale sale, PharmacyStock stock, int quantity, QuickPharmacySaleRequest.Item itemDto) {

    Integer availableStock = stock.getQuantity();
    if (availableStock <= 0) {
      throw new ApiException("OUT_OF_STOCK", "Medicine is out of stock", HttpStatus.BAD_REQUEST);
    }
    if (quantity <= 0) {
      throw new ApiException(
          "INVALID_QUANTITY", "Quantity must be greater than zero", HttpStatus.BAD_REQUEST);
    }
    if (quantity > availableStock) {
      throw new ApiException(
          "INSUFFICIENT_STOCK",
          "Insufficient stock for " + stock.getMedicine().getName(),
          HttpStatus.CONFLICT);
    }

    stock.setQuantity(stock.getQuantity() - quantity);
    pharmacyStockRepository.save(stock);

    BigDecimal unitPrice = stock.getSellingPrice();

    PharmacySaleItem saleItem = new PharmacySaleItem();
    saleItem.setSale(sale);
    saleItem.setMedicine(stock.getMedicine());
    saleItem.setStock(stock);
    saleItem.setQuantity(quantity);
    saleItem.setUnitPrice(unitPrice);
    saleItem.setDiscountAmount(
        itemDto.getDiscountAmount() != null ? itemDto.getDiscountAmount() : BigDecimal.ZERO);
    saleItem.setTaxAmount(BigDecimal.ZERO);
    BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    saleItem.setSubtotal(itemSubtotal);
    if (saleItem.getDiscountAmount().compareTo(itemSubtotal) > 0) {
      throw new ApiException(
          "INVALID_DISCOUNT",
          "Discount amount cannot exceed medicine total price",
          HttpStatus.BAD_REQUEST);
    }

    BigDecimal itemNet =
        itemSubtotal.add(saleItem.getTaxAmount()).subtract(saleItem.getDiscountAmount());
    saleItem.setTaxableAmount(itemSubtotal.subtract(saleItem.getDiscountAmount()));
    saleItem.setNetAmount(itemNet);

    sale.getItems().add(saleItem);
    return itemNet;
  }

  private Invoice createInvoice(
      QuickPharmacySaleRequest request, UUID hospitalId, PharmacySale savedSale) {
    Invoice invoice =
        invoiceService.getOrCreateInvoice(
            hospitalId,
            request.getPatientId(),
            savedSale.getVisitId(),
            savedSale.getCustomerName(),
            savedSale.getCustomerPhone());
    savedSale.setInvoiceId(invoice.getId());
    savedSale.setSaleNumber(invoice.getInvoiceNumber());
    return invoice;
  }

  private void addInvoiceItems(PharmacySale savedSale, Invoice invoice) {
    String doctorName = savedSale.getExternalDoctorName();
    if (doctorName == null && savedSale.getDoctorId() != null) {
      doctorName =
          userAccountRepository
              .findById(savedSale.getDoctorId())
              .map(com.res.hms.iam.domain.UserAccount::getFullName)
              .orElse(null);
    }

    for (PharmacySaleItem saleItem : savedSale.getItems()) {
      List<com.res.hms.tax.application.dto.TaxComponent> taxComponents =
          saleItem.getTaxes().stream()
              .map(
                  t ->
                      new com.res.hms.tax.application.dto.TaxComponent(
                          t.getTaxCode(), t.getTaxName(), t.getTaxRate(), t.getTaxAmount()))
              .toList();

      invoiceService.addItem(
          invoice.getId(),
          saleItem.getMedicine().getName() + " (" + saleItem.getStock().getBatchNumber() + ")",
          InvoiceItemCategory.PHARMACY,
          BigDecimal.valueOf(saleItem.getQuantity()),
          saleItem.getUnitPrice(),
          saleItem.getDiscountAmount(),
          FinanceSourceType.PHARMACY_SALE_ITEM,
          saleItem.getId(),
          savedSale.getDoctorId(),
          doctorName,
          taxComponents);
    }
  }

  private PaymentReceipt recordPayment(
      QuickPharmacySaleRequest request,
      UUID hospitalId,
      UUID userId,
      Invoice invoice,
      BigDecimal saleAmount) {

    if (request.getPayment() != null
        && request.getPayment().getAmount() != null
        && request.getPayment().getAmount().compareTo(BigDecimal.ZERO) > 0) {

      RecordPaymentRequest paymentReq = new RecordPaymentRequest();
      paymentReq.setInvoiceId(invoice.getId());
      paymentReq.setPatientId(request.getPatientId());
      paymentReq.setVisitId(request.getVisitId());
      paymentReq.setAmount(request.getPayment().getAmount());
      paymentReq.setPaymentMode(request.getPayment().getMode());
      paymentReq.setReferenceNumber(request.getPayment().getReferenceNumber());

      // Validate that payment is at least the pharmacy sale amount
      if (request.getPayment().getAmount().compareTo(saleAmount) < 0) {
        throw new ApiException(
            "INVALID_PAYMENT",
            "Paid amount is less than pharmacy bill amount",
            HttpStatus.BAD_REQUEST);
      }

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
                        .taxableAmount(item.getTaxableAmount())
                        .taxAmount(item.getTaxAmount())
                        .discountAmount(item.getDiscountAmount())
                        .netAmount(item.getNetAmount())
                        .appliedTaxes(
                            item.getTaxes() != null
                                ? item.getTaxes().stream()
                                    .map(
                                        t ->
                                            new com.res.hms.tax.application.dto.TaxComponent(
                                                t.getTaxCode(),
                                                t.getTaxName(),
                                                t.getTaxRate(),
                                                t.getTaxAmount()))
                                    .toList()
                                : null)
                        .build())
            .toList();

    QuickPharmacySaleResponse response =
        QuickPharmacySaleResponse.builder()
            .saleId(sale.getId())
            .saleNumber(sale.getSaleNumber())
            .invoiceId(sale.getInvoiceId())
            .invoiceNumber(sale.getSaleNumber())
            .totalAmount(sale.getTotalAmount())
            .status(sale.getStatus().name())
            .customerName(sale.getCustomerName())
            .customerPhone(sale.getCustomerPhone())
            .externalDoctorName(sale.getExternalDoctorName())
            .createdAt(sale.getCreatedAt())
            .items(items)
            .build();

    if (sale.getDoctorId() != null) {
      userAccountRepository
          .findById(sale.getDoctorId())
          .ifPresent(u -> response.setDoctorName(u.getFullName()));
    }
    if (sale.getPatientId() != null) {
      patientRepository
          .findById(sale.getPatientId())
          .ifPresent(p -> response.setPatientCode(p.getPatientCode()));
    }

    return response;
  }

  @Transactional(readOnly = true)
  public List<Object[]> searchRecentCustomers(String search) {
    UUID hospitalId = hospitalContext.requireHospitalId();
    return pharmacySaleRepository.searchRecentWalkinCustomers(hospitalId, search);
  }
}
