package com.res.hms.user.dto;

import com.res.hms.doctor.dto.DoctorEducation;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserFullProfileDTO extends UserSummaryDTO {
    
    private DoctorInfo doctorInfo;
    private NurseInfo nurseInfo;
    private List<DetailedAssignmentDTO> assignmentHistory;
    
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorInfo {
        private String specialization;
        private DoctorEducation education;
        private String bio;
        private String licenseNumber;
    }

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NurseInfo {
        private String department;
        private String nursingLicenseNo;
        private String certificationLevel;
    }
}
