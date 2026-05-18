package com.res.hms.iam.application.dto;

import com.res.hms.core.domain.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDTO {
  private UUID id;
  private String username;
  private String email;
  private String fullName;
  private Gender gender;

  @Schema(type = "string", example = "1990-01-01")
  private LocalDate dob;

  private String phone;
  private boolean active;
  private boolean deleted;
  private boolean superuser;

  private List<HospitalMembershipDTO> memberships;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class HospitalMembershipDTO {
    private UUID hospitalId;
    private String hospitalName;
    private List<String> roles;
    private List<String> specializations;
    private List<String> departments;
  }
}
