package com.res.hms.pharmacy.application.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImportResponseDto {
  private int totalRows;
  private int successCount;
  private int failureCount;
  private boolean errorFileAvailable;
  private String errorFileBase64;
}
