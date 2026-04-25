package com.res.hms.search.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class SearchResultDTO {
    private UUID id;
    private String type; // HOSPITAL, USER, PATIENT
    private String title;
    private String subtitle;
    private String link;
}
