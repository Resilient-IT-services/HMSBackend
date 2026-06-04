package com.res.hms.lab.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "lab_result_parameter", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabResultParameter {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "result_id")
  private UUID resultId;

  @Column(name = "parameter_id")
  private UUID parameterId;

  @Column(name = "result_value")
  private String resultValue;

  @Column(name = "abnormal")
  private Boolean abnormal;

  @Column(name = "critical")
  private Boolean critical;

  @Column(name = "result_status")
  private String resultStatus;
}
