package com.res.hms.pharmacy.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PharmacyReturnResponse {
  private UUID id;
  private UUID saleItemId;
  private UUID stockId;
  private Integer quantity;
  private String reason;
}
