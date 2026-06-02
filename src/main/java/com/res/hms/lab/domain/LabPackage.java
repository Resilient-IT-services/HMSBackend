package com.res.hms.lab.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "lab_package", schema = "lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabPackage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hospital_id")
    private UUID hospitalId;

    @Column(name = "package_code")
    private String packageCode;

    @Column(name = "package_name")
    private String packageName;

    @Column(name = "active")
    private Boolean active;
}
