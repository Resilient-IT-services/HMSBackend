package com.res.hms.hospital.application.dto;

import com.res.hms.hospital.domain.HospitalStatus;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
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
  private String city;
  private String state;
  private String pincode;
  private String gstin;
  private String registrationNo;

  @NotBlank(message = "timezone is required")
  private String timezone;

  private HospitalStatus status;
  private HospitalSettings settings;
  private List<String> departments;
}
