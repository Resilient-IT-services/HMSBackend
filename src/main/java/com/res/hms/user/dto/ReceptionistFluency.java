package com.res.hms.user.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceptionistFluency {
    private List<LanguageEntry> fluencies;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LanguageEntry {
        private String language;
        private String proficiency; // e.g., NATIVE, FLUENT, INTERMEDIATE
    }
}
