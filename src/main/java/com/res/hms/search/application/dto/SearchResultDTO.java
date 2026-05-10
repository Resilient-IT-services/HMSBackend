package com.res.hms.search.application.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchResultDTO {
  private UUID id;
  private String type; // HOSPITAL, USER, PATIENT
  private String title;
  private String subtitle;
  private String link;
}
