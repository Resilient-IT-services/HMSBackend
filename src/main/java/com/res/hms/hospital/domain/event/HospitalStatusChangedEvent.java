package com.res.hms.hospital.domain.event;

import com.res.hms.hospital.domain.HospitalStatus;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HospitalStatusChangedEvent {
  private final UUID hospitalId;
  private final HospitalStatus oldStatus;
  private final HospitalStatus newStatus;
}
