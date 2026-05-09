package com.res.hms.pharmacy.application;

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
  private int medicineNotFoundRows;
}
