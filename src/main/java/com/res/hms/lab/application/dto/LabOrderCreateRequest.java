package com.res.hms.lab.application.dto;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabOrderCreateRequest {
    private UUID patientId;
    private UUID encounterId;
    private List<UUID> testIds;
    private List<UUID> packageIds;
}
