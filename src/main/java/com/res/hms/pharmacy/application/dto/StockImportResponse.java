package com.res.hms.pharmacy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockImportResponse {
  private int totalRows;
  private int importedRows;
  private int skippedRows;
  private int failedRows;
  private int medicineNotFoundRows;
  private String lastError;
  private String errorFileBase64;
}
