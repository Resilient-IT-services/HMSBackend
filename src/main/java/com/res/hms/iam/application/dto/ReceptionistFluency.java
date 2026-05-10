package com.res.hms.iam.application.dto;

import java.util.List;

import com.res.hms.core.domain.ProficiencyLevel;

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
    private ProficiencyLevel proficiency; // e.g., NATIVE, FLUENT, INTERMEDIATE
  }
}
