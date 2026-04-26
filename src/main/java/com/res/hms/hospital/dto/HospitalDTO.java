package com.res.hms.hospital.dto;

import com.res.hms.domain.HospitalStatus;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalDTO {
  private UUID id;

  @NotBlank(message = "name is required")
  private String name;

  @NotBlank(message = "code is required")
  private String code;

  private String address;
  private String gstin;
  private HospitalStatus status;
  private HospitalSettings settings;
}
