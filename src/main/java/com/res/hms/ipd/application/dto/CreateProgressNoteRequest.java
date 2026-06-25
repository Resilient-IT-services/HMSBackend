package com.res.hms.ipd.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateProgressNoteRequest {
  @NotBlank(message = "Note is required")
  private String note;
}
