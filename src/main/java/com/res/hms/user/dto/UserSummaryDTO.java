package com.res.hms.user.dto;

import com.res.hms.domain.Gender;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
  private LocalDate dob;
  private String phone;
  private boolean active;
  private boolean deleted;
  private boolean superuser;
  private LocalDateTime createdAt;
  private List<HospitalMembershipDTO> memberships;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class HospitalMembershipDTO {
    private UUID hospitalId;
    private List<String> roles;
    private String specialization;
    private String departmentName;
  }
}
