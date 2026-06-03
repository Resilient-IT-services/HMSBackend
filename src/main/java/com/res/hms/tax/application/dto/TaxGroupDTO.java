package com.res.hms.tax.application.dto;

import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class TaxGroupDTO {
  private UUID id;
  private String name;
  private boolean active;
  private List<TaxMasterDTO> taxes;
}
