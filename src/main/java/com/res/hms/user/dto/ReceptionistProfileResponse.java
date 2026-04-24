package com.res.hms.user.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceptionistProfileResponse {
  private UUID userId;
  private ReceptionistFluency languagesSpoken;
  private boolean insuranceSpecialist;
}
