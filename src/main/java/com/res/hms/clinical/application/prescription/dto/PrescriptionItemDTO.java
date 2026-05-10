package com.res.hms.clinical.application.prescription.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionItemDTO {
  private UUID id;
  private UUID medicineId;
  private String medicineName;
  private String dosage;
  private String duration;
}
