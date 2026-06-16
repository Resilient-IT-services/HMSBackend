package com.res.hms.pharmacy.application.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ConsumableImportDto {
  private int rowNumber;
  private String department;
  private String category;
  private String itemCode;
  private String itemName;
  private String baseUnit;
  private String manufacturer;
  private String lowStockAlert;
  private String isBatchTracked;
  private String isExpiryTracked;
  private List<String> errors = new ArrayList<>();

  public void addError(String msg) {
    this.errors.add(msg);
  }

  public String getErrorLog() {
    return String.join(", ", errors);
  }
}
