package com.res.hms.tax.application.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Data;

@Data
public class TaxRuleDTO {
  private UUID id;
  private int priority;
  private String itemType;
  private String hsnCode;
  private String sacCode;
  private String patientCategory;
  private UUID taxGroupId;
  private String taxGroupName;
  private Instant effectiveFrom;
  private Instant effectiveTo;
}
