package com.res.hms.pharmacy.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicineImportResponse {
  private int totalRows;
  private int importedRows;
  private int skippedRows;
}
