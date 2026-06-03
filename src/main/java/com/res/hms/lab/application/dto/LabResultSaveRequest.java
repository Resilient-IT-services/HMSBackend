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
public class LabResultSaveRequest {
    private UUID orderTestId;
    private List<ParameterResultDTO> results;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ParameterResultDTO {
        private UUID parameterId;
        private String value;
        private Boolean abnormal;
        private Boolean critical;
    }
}
