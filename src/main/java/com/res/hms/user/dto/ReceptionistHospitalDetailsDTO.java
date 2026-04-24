package com.res.hms.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceptionistHospitalDetailsDTO {
    private String deskLocation;
    private String emergencyContactNumber;
}
