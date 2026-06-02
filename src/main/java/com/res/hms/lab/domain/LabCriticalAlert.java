package com.res.hms.lab.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "lab_critical_alert", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabCriticalAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "result_parameter_id")
    private UUID resultParameterId;

    @Column(name = "alert_message")
    private String alertMessage;

    @Column(name = "created_at")
    private Instant createdAt;
}
