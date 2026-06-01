package com.res.hms.finance.application.dto;

import com.res.hms.finance.domain.InvoiceItemCategory;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItemDTO {
  private UUID id;
  private String itemDescription;
  private InvoiceItemCategory category;
  private BigDecimal quantity;
  private BigDecimal unitPrice;
  private BigDecimal subtotal;
  private BigDecimal taxableAmount;
  private BigDecimal taxAmount;
  private BigDecimal discountAmount;
  private BigDecimal netTotal;
  private com.res.hms.finance.domain.FinanceSourceType sourceType;
  private UUID sourceId;
  private java.util.List<com.res.hms.tax.application.dto.TaxComponent> appliedTaxes;
}
