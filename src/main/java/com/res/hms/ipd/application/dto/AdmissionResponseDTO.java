package com.res.hms.ipd.application.dto;

import com.res.hms.ipd.domain.AdmissionStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;

@Data
public class AdmissionResponseDTO {
  private UUID id;
  private String admissionNumber;
  private UUID patientId;
  private String patientName;
  private UUID doctorId;
  private String doctorName;
  private UUID bedId;
  private String bedNumber;
  private String wardName;
  private Instant admissionDateTime;
  private AdmissionStatus status;
  private String admissionReason;
  private String remarks;
}
