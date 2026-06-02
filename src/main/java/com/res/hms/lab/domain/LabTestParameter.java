package com.res.hms.lab.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "lab_test_parameter", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabTestParameter {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "test_id")
    private UUID testId;

    @Column(name = "parameter_name")
    private String parameterName;

    @Column(name = "unit")
    private String unit;

    @Column(name = "display_order")
    private Integer displayOrder;
}
