package com.res.hms.clinical.application.patient.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PatientDashboardStatsDTO {

    private long totalRecords;

    private long newToday;

    private long inPatient;
}
