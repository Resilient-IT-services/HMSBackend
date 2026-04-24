package com.res.hms.hospital.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalSettings {
    private Map<String, Object> configurations;
    private String timezone;
    private String currency;
    private Boolean opdEnabled;
    private Boolean pharmacyEnabled;
}
