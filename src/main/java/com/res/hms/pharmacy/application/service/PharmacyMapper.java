package com.res.hms.pharmacy.application.service;

import com.res.hms.pharmacy.application.dto.PharmacyStockDTO;
import com.res.hms.pharmacy.application.dto.QuickPharmacySaleResponse;
import com.res.hms.pharmacy.domain.PharmacySale;
import com.res.hms.pharmacy.domain.PharmacyStock;
import com.res.hms.pharmacy.infrastructure.repository.PharmacyStockRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PharmacyMapper {

  private final PharmacyStockRepository pharmacyStockRepository;

  public QuickPharmacySaleResponse toQuickSaleResponse(PharmacySale sale) {
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

    return QuickPharmacySaleResponse.builder()
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
  }

  public PharmacyStockDTO toStockDto(PharmacyStock stock) {
    Long total =
        pharmacyStockRepository.availableQuantity(
            stock.getHospitalId(), stock.getMedicine().getId(), LocalDate.now());
    return PharmacyStockDTO.builder()
        .id(stock.getId())
        .medicineId(stock.getMedicine().getId())
        .medicineName(stock.getMedicine().getName())
        .genericName(stock.getMedicine().getGenericName())
        .composition(stock.getMedicine().getComposition())
        .batchNumber(stock.getBatchNumber())
        .expiryDate(stock.getExpiryDate())
        .quantity(stock.getQuantity())
        .totalQuantity(total.intValue())
        .purchasePrice(stock.getPurchasePrice())
        .sellingPrice(stock.getSellingPrice())
        .price(stock.getSellingPrice())
        .build();
  }
}
