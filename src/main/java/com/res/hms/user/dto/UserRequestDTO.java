package com.res.hms.user.dto;

import com.res.hms.domain.Gender;
import com.res.hms.domain.RoleCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDTO {
    @NotBlank(message = "username is required")
    private String username;
    
    private String password;
    
    @NotBlank(message = "fullName is required")
    private String fullName;
    
    @NotNull(message = "gender is required")
    private Gender gender;
    
    @NotNull(message = "dob is required")
    private LocalDate dob;
    
    private String phone;
    private List<MembershipRequestDTO> memberships;
    private Boolean superuser;
    
    // Profile specific fields (optional, used if specific roles are assigned)
    private String specialization;
    private String department;
    private BigDecimal consultationFee;
    private Integer followUpDays;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MembershipRequestDTO {
        private UUID hospitalId;
        private Set<RoleCode> roles;
    }
}
