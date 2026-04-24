package com.res.hms.doctor.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorEducation {
    private List<Qualification> qualifications;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Qualification {
        private String degree;
        private String institution;
        private Integer year;
        private String specialization;
    }
}
