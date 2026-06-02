package com.res.hms.lab.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "lab_result", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabResult {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sample_id")
    private UUID sampleId;

    @Column(name = "entered_by")
    private UUID enteredBy;

    @Column(name = "entered_at")
    private Instant enteredAt;
}
