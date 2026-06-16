package com.res.hms.pharmacy.application.mapper;

import com.res.hms.clinical.infrastructure.repository.PatientRepository;
import com.res.hms.iam.infrastructure.repository.UserAccountRepository;
import com.res.hms.pharmacy.application.dto.QuickPharmacySaleResponse;
import com.res.hms.pharmacy.domain.PharmacySale;
import com.res.hms.pharmacy.domain.PharmacySaleItem;
import com.res.hms.tax.application.dto.TaxComponent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PharmacySaleMapper {

  private final UserAccountRepository userAccountRepository;
  private final PatientRepository patientRepository;

  public QuickPharmacySaleResponse toResponse(PharmacySale sale) {
    List<QuickPharmacySaleResponse.Item> items =
        sale.getItems().stream().map(this::toItemResponse).toList();

    QuickPharmacySaleResponse response =
        QuickPharmacySaleResponse.builder()
            .saleId(sale.getId())
            .saleNumber(sale.getSaleNumber())
            .invoiceId(sale.getInvoiceId())
            .invoiceNumber(sale.getSaleNumber())
            .totalAmount(sale.getTotalAmount())
            .status(sale.getStatus().name())
            .saleType(sale.getSaleType() != null ? sale.getSaleType().name() : null)
            .customerName(sale.getCustomerName())
            .customerPhone(sale.getCustomerPhone())
            .externalDoctorName(sale.getExternalDoctorName())
            .createdAt(sale.getCreatedAt())
            .items(items)
            .build();

    enrichResponse(sale, response);
    return response;
  }

  private QuickPharmacySaleResponse.Item toItemResponse(PharmacySaleItem item) {
    return QuickPharmacySaleResponse.Item.builder()
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
                            new TaxComponent(
                                t.getTaxCode(), t.getTaxName(), t.getTaxRate(), t.getTaxAmount()))
                    .toList()
                : null)
        .build();
  }

  private void enrichResponse(PharmacySale sale, QuickPharmacySaleResponse response) {
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
  }
}
