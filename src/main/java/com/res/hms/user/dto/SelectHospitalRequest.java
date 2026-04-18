package com.res.hms.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record SelectHospitalRequest(
        @NotNull(message = "hospital_id is required")
        @JsonProperty("hospital_id")
        Long hospitalId
) {
}
