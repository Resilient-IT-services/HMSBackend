package com.res.hms.tax.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;

@Data
public class TaxMasterDTO {
  private UUID id;
  private String code;
  private String name;
  private String taxType;
  private BigDecimal rate;
  private boolean active;
  private Instant effectiveFrom;
  private Instant effectiveTo;
}
