package com.res.hms.lab.application.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabTestParameterDTO {
    private UUID id;
    private UUID testId;
    private String parameterName;
    private String unit;
    private Integer displayOrder;
}
