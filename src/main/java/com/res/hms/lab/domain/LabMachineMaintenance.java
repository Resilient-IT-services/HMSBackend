package com.res.hms.lab.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "lab_machine_maintenance", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabMachineMaintenance {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "machine_id")
  private UUID machineId;

  @Column(name = "maintenance_date")
  private LocalDate maintenanceDate;

  @Column(name = "next_due_date")
  private LocalDate nextDueDate;

  @Column(name = "remarks")
  private String remarks;
}
