package com.res.hms.audit.application.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditDetail {
  private Map<String, Object> before;
  private Map<String, Object> after;
}
